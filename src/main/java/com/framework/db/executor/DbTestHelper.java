package com.framework.db.executor;

import com.framework.db.queries.DbQueries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reusable DB setup and teardown helpers for test classes.
 *
 * Wraps common "do this before/after a test" DB operations
 * so test code stays clean and doesn't contain raw SQL.
 *
 * Usage in a test @AfterMethod:
 *   dbHelper.cleanupTestUser(email);
 *   dbHelper.cleanupTestUsersByPattern("test_%@auto.in.com");
 *
 * Usage in a test @BeforeMethod:
 *   dbHelper.forceUserStatus(userId, "ACTIVE");
 */
public class DbTestHelper {

    private static final Logger log = LogManager.getLogger(DbTestHelper.class);

    private final DbClient pgDb;
    private final DbClient orDb;  // may be null if Oracle not configured

    public DbTestHelper(DbClient pgDb, DbClient orDb) {
        this.pgDb = pgDb;
        this.orDb = orDb;
    }

    // ── Cleanup helpers (PostgreSQL) ──────────────────────────────────────────

    /**
     * Hard-deletes a user from Postgres by email.
     * Use in @AfterMethod to clean up test-generated users.
     */
    public void cleanupTestUser(String email) {
        if (pgDb == null) return;
        int rows = pgDb.execute(DbQueries.User.DELETE_BY_ID +
                " /* via email lookup */", email);
        log.info("Cleanup: deleted {} user row(s) for email={}", rows, email);
    }

    /**
     * Deletes all users whose email matches the given LIKE pattern.
     * Useful for bulk cleanup after a test suite.
     *
     * @param pattern SQL LIKE pattern e.g. "test_%@auto.in.com"
     */
    public void cleanupTestUsersByPattern(String pattern) {
        if (pgDb == null) return;
        int rows = pgDb.execute(DbQueries.User.DELETE_TEST_USERS, pattern);
        log.info("Cleanup: deleted {} test user(s) matching pattern='{}'", rows, pattern);
    }

    /**
     * Deletes a specific user by userId (hard delete).
     */
    public void deleteUserById(String userId) {
        if (pgDb == null) return;
        int rows = pgDb.execute(DbQueries.User.DELETE_BY_ID, userId);
        log.info("Cleanup: deleted userId={} → {} row(s)", userId, rows);
    }

    // ── State manipulation helpers ────────────────────────────────────────────

    /**
     * Force-sets a user's status directly in DB.
     * Useful to set up preconditions (e.g. INACTIVE user before a test).
     */
    public void forceUserStatus(String userId, String status) {
        if (pgDb == null) return;
        int rows = pgDb.execute(DbQueries.User.UPDATE_STATUS, status, userId);
        log.info("forceUserStatus: userId={} → status={} ({} row(s))", userId, status, rows);
    }

    // ── Existence checks ──────────────────────────────────────────────────────

    /**
     * Returns true if a user with this email exists in Postgres.
     */
    public boolean userExistsByEmail(String email) {
        if (pgDb == null) return false;
        Number count = pgDb.queryScalar(DbQueries.User.EXISTS_BY_EMAIL, email);
        return count != null && count.longValue() > 0;
    }

    /**
     * Returns true if an order exists for the given orderId in Postgres.
     */
    public boolean orderExists(String orderId) {
        if (pgDb == null) return false;
        return pgDb.queryOne(DbQueries.Order.FIND_BY_ID, orderId).isPresent();
    }

    // ── Oracle helpers ────────────────────────────────────────────────────────

    /**
     * Returns the count of audit events for a given entity in Oracle.
     * Returns 0 if Oracle is not configured.
     */
    public long auditEventCount(String entityId, String eventType) {
        if (orDb == null) return 0;
        Number count = orDb.queryScalar(
                DbQueries.Audit.COUNT_EVENTS_BY_ENTITY, entityId, eventType);
        return count != null ? count.longValue() : 0;
    }
}
