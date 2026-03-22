package com.framework.tests.db;

import com.framework.db.mapper.OrderDbRecord;
import com.framework.db.mapper.UserDbRecord;
import com.framework.db.queries.DbQueries;
import com.framework.tests.BaseApiTest;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure DB validation tests — no UI, no API calls.
 * Extends BaseApiTest so no browser is started.
 *
 * Demonstrates:
 *  TC01 — Fluent DbValidator: verify user record exists with correct fields
 *  TC02 — Typed RowMapper: query with UserDbRecord POJO
 *  TC03 — Scalar / COUNT assertions
 *  TC04 — Oracle audit log cross-validation
 *  TC05 — Multi-row assertion (user's orders)
 *  TC06 — Null-field check (deleted_at should be null for active user)
 *  TC07 — Batch insert + count verification
 *  TC08 — Stored procedure call
 *  TC09 — Postgres ↔ Oracle sync check (same count on both DBs)
 *
 * Precondition: The test user below must exist in your QA database.
 * In a real project these IDs come from test data files or @BeforeMethod API setup.
 */
public class UserDbTest extends BaseApiTest {

    // These would normally come from a @DataProvider or TestContext after API setup
    private static final String KNOWN_USER_ID = "usr_test_001";
    private static final String KNOWN_EMAIL   = "qa_user_in@myapp.in";
    private static final String TEST_EMAIL_PATTERN = "test_%@auto.in.com";

    // ── TC01: Fluent DbValidator — user exists with correct status ────────────
    @Test(
        description = "DB: user record exists in Postgres with correct fields",
        groups = { "db", "smoke" }
    )
    public void testUserExistsInPostgres() {
        dbAssert(pgDb)
            .forQuery(DbQueries.User.FIND_BY_ID, KNOWN_USER_ID)
            .rowExists()
            .columnEquals("status",  "ACTIVE")
            .columnEquals("country", country())
            .columnNotBlank("email")
            .columnNotBlank("created_at")
            .columnIsNull("deleted_at")     // not soft-deleted
            .validate();
    }

    // ── TC02: Typed RowMapper — query returns strongly-typed POJO ─────────────
    @Test(
        description = "DB: query returns typed UserDbRecord POJO via RowMapper",
        groups = { "db", "regression" }
    )
    public void testUserRowMappedToTypedPojo() {
        Optional<UserDbRecord> result = pgDb.queryOne(
                DbQueries.User.FIND_BY_EMAIL,
                UserDbRecord.rowMapper(),
                KNOWN_EMAIL);

        assertThat(result)
            .as("User with email %s should exist in DB", KNOWN_EMAIL)
            .isPresent();

        UserDbRecord user = result.get();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(user.getEmail())    .isEqualTo(KNOWN_EMAIL);
        soft.assertThat(user.getStatus())   .isEqualTo("ACTIVE");
        soft.assertThat(user.getCountry())  .isEqualTo(country());
        soft.assertThat(user.getUserId())   .isNotBlank();
        soft.assertThat(user.getCreatedAt()).isNotNull();
        soft.assertAll();
    }

    // ── TC03: Scalar / COUNT ──────────────────────────────────────────────────
    @Test(
        description = "DB: COUNT query returns exactly 1 for a known email",
        groups = { "db", "smoke" }
    )
    public void testUserCountByEmail() {
        dbAssert(pgDb)
            .forQuery(DbQueries.User.EXISTS_BY_EMAIL, KNOWN_EMAIL)
            .scalarEquals(1L)
            .validate();
    }

    @Test(
        description = "DB: COUNT returns 0 for a non-existent email",
        groups = { "db", "negative" }
    )
    public void testNonExistentUserCountIsZero() {
        dbAssert(pgDb)
            .forQuery(DbQueries.User.EXISTS_BY_EMAIL, "ghost_user_xyz@nowhere.com")
            .scalarEquals(0L)
            .validate();
    }

    // ── TC04: Oracle audit log validation ─────────────────────────────────────
    @Test(
        description = "Oracle: USER_CREATED audit event exists for known user",
        groups = { "db", "regression" }
    )
    public void testAuditLogEntryInOracle() {
        // Guard: only run if Oracle is configured for this env+country
        if (orDb == null) {
            log.warn("Oracle not configured for env={} country={} — skipping Oracle test",
                    config().getEnvironment(), country());
            return;
        }

        dbAssert(orDb)
            .forQuery(DbQueries.Audit.COUNT_EVENTS_BY_ENTITY, KNOWN_USER_ID, "USER_CREATED")
            .scalarEquals(1L)
            .validate();
    }

    @Test(
        description = "Oracle: latest audit event for user is USER_CREATED",
        groups = { "db", "regression" }
    )
    public void testLatestAuditEventInOracle() {
        if (orDb == null) {
            log.warn("Oracle not configured — skipping");
            return;
        }

        // Use Oracle-specific query (ROWNUM instead of LIMIT)
        dbAssert(orDb)
            .forQuery(DbQueries.Audit.LATEST_EVENT_ORA, KNOWN_USER_ID)
            .rowExists()
            .columnEquals("event_type", "USER_CREATED")
            .columnEquals("entity_id",  KNOWN_USER_ID)
            .validate();
    }

    // ── TC05: Multi-row assertion — user's orders ─────────────────────────────
    @Test(
        description = "DB: user has at least 1 order in Postgres",
        groups = { "db", "regression" }
    )
    public void testUserHasOrders() {
        dbAssert(pgDb)
            .forQuery(DbQueries.Order.FIND_BY_USER, KNOWN_USER_ID)
            .rowCountAtLeast(1)
            .validate();
    }

    @Test(
        description = "DB: each order row for user has required fields populated",
        groups = { "db", "regression" }
    )
    public void testOrderRowsAreWellFormed() {
        List<Map<String, Object>> orders = pgDb.query(
                DbQueries.Order.FIND_BY_USER, KNOWN_USER_ID);

        assertThat(orders).isNotEmpty();

        SoftAssertions soft = new SoftAssertions();
        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> row = orders.get(i);
            soft.assertThat(row.get("order_id"))
                .as("orders[%d].order_id should not be null", i).isNotNull();
            soft.assertThat(row.get("status"))
                .as("orders[%d].status should not be null", i).isNotNull();
            soft.assertThat(row.get("total_amount"))
                .as("orders[%d].total_amount should not be null", i).isNotNull();
        }
        soft.assertAll();
    }

    @Test(
        description = "DB: ORDER COUNT via scalar matches LIST query row count",
        groups = { "db", "regression" }
    )
    public void testOrderCountConsistency() {
        // Scalar COUNT
        Number countFromScalar = pgDb.queryScalar(
                DbQueries.Order.COUNT_BY_USER, KNOWN_USER_ID);

        // Row list
        List<Map<String, Object>> rows = pgDb.query(
                DbQueries.Order.FIND_BY_USER, KNOWN_USER_ID);

        assertThat(countFromScalar.longValue())
            .as("Scalar COUNT(*) should match actual list size")
            .isEqualTo((long) rows.size());
    }

    // ── TC06: Typed mapper for orders ─────────────────────────────────────────
    @Test(
        description = "DB: OrderDbRecord typed mapper returns correct fields",
        groups = { "db", "regression" }
    )
    public void testOrderTypedMapper() {
        List<OrderDbRecord> orders = pgDb.query(
                DbQueries.Order.FIND_BY_USER,
                OrderDbRecord.rowMapper(),
                KNOWN_USER_ID);

        assertThat(orders).isNotEmpty();

        OrderDbRecord first = orders.get(0);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(first.getOrderId())   .isNotBlank();
        soft.assertThat(first.getUserId())    .isEqualTo(KNOWN_USER_ID);
        soft.assertThat(first.getStatus())    .isNotBlank();
        soft.assertThat(first.getCreatedAt()) .isNotNull();
        soft.assertAll();
    }

    // ── TC07: Batch insert + count verification ───────────────────────────────
    @Test(
        description = "DB: batch insert test records then verify COUNT, then cleanup",
        groups = { "db", "regression" }
    )
    public void testBatchInsertAndCount() {
        String tag = "batch_" + System.currentTimeMillis();

        // Prepare 3 batch rows
        List<Object[]> rows = List.of(
            new Object[]{ tag + "_1@auto.test.com", "TEST_BATCH", country() },
            new Object[]{ tag + "_2@auto.test.com", "TEST_BATCH", country() },
            new Object[]{ tag + "_3@auto.test.com", "TEST_BATCH", country() }
        );

        String insertSql =
            "INSERT INTO users (email, status, country, created_at) " +
            "VALUES (?, ?, ?, NOW())";

        int[] affected = pgDb.executeBatch(insertSql, rows);
        assertThat(affected).hasSize(3);

        // Verify all 3 were inserted
        dbAssert(pgDb)
            .forQuery("SELECT COUNT(*) FROM users WHERE email LIKE ?", tag + "%")
            .scalarEquals(3L)
            .validate();

        // Cleanup
        pgDb.execute("DELETE FROM users WHERE email LIKE ?", tag + "%");
    }

    // ── TC08: Stored procedure call ───────────────────────────────────────────
    @Test(
        description = "DB: call stored procedure to get user count for country",
        groups = { "db" }
    )
    public void testStoredProcedureCall() {
        // Postgres: { call get_active_user_count(?, ?) }
        // where param1=IN (country), param2=OUT (count)
        // Adjust the procedure name and params to match your schema.
        Object result = pgDb.callProcedure(
                "{ call get_active_user_count(?) }", country());

        // If your DB has this proc, assert the result
        // assertThat(result).isNotNull();
        // assertThat(((Number) result).longValue()).isGreaterThan(0);

        log.info("Stored proc result for country={}: {}", country(), result);
        // Placeholder — no assertion until proc exists in your schema
    }

    // ── TC09: Postgres ↔ Oracle sync check ───────────────────────────────────
    @Test(
        description = "DB: user COUNT in Postgres matches replicated count in Oracle",
        groups = { "db", "regression" }
    )
    public void testPostgresOracleSyncConsistency() {
        if (orDb == null) {
            log.warn("Oracle not configured for this env/country — skipping sync check");
            return;
        }

        Number pgCount = pgDb.queryScalar(
                "SELECT COUNT(*) FROM users WHERE country = ? AND status = 'ACTIVE'",
                country());

        Number orCount = orDb.queryScalar(
                "SELECT COUNT(*) FROM users WHERE country = ? AND status = 'ACTIVE'",
                country());

        assertThat(pgCount).isNotNull();
        assertThat(orCount).isNotNull();

        assertThat(pgCount.longValue())
            .as("Postgres active user count (%d) should match Oracle (%d) for country=%s",
                pgCount.longValue(), orCount.longValue(), country())
            .isEqualTo(orCount.longValue());
    }
}
