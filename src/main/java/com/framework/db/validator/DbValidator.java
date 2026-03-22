package com.framework.db.validator;

import com.framework.db.executor.DbClient;
import com.framework.db.mapper.GenericRowMapper;
import com.framework.db.mapper.RowMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fluent DB assertion chain.
 *
 * This is the "ResponseValidator equivalent" for databases.
 * It wraps a DbClient and exposes readable assertion methods
 * so test code never contains raw SQL + manual assertions.
 *
 * All assertions are collected as soft assertions; call validate() at the end.
 *
 * Usage pattern A — record-level:
 *   DbValidator.using(pgDb)
 *       .forQuery("SELECT * FROM users WHERE user_id = ?", userId)
 *       .rowExists()
 *       .columnEquals("status", "ACTIVE")
 *       .columnEquals("country", "IN")
 *       .validate();
 *
 * Usage pattern B — count / aggregate:
 *   DbValidator.using(pgDb)
 *       .forQuery("SELECT COUNT(*) FROM orders WHERE user_id = ?", userId)
 *       .scalarEquals(3L)
 *       .validate();
 *
 * Usage pattern C — multiple rows:
 *   DbValidator.using(pgDb)
 *       .forQuery("SELECT * FROM audit_log WHERE user_id = ?", userId)
 *       .rowCountEquals(2)
 *       .validate();
 */
public class DbValidator {

    private static final Logger log = LogManager.getLogger(DbValidator.class);

    private final DbClient      dbClient;
    private final SoftAssertions soft = new SoftAssertions();

    // lazily loaded result
    private String           sql;
    private Object[]         params;
    private List<Map<String, Object>> rows;
    private boolean          resultLoaded = false;

    private DbValidator(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    // ── Entry points ──────────────────────────────────────────────────────────

    public static DbValidator using(DbClient dbClient) {
        return new DbValidator(dbClient);
    }

    /**
     * Sets the query to execute. Results are loaded lazily on first assertion call.
     */
    public DbValidator forQuery(String sql, Object... params) {
        this.sql    = sql;
        this.params = params;
        return this;
    }

    // ── Row existence ─────────────────────────────────────────────────────────

    /** Assert at least one row was returned. */
    public DbValidator rowExists() {
        loadIfNeeded();
        soft.assertThat(rows)
            .as("DB query should return at least one row\nSQL: %s", sql)
            .isNotEmpty();
        return this;
    }

    /** Assert no rows were returned. */
    public DbValidator rowNotExists() {
        loadIfNeeded();
        soft.assertThat(rows)
            .as("DB query should return no rows\nSQL: %s", sql)
            .isEmpty();
        return this;
    }

    // ── Row count ─────────────────────────────────────────────────────────────

    /** Assert exact number of rows returned. */
    public DbValidator rowCountEquals(int expected) {
        loadIfNeeded();
        soft.assertThat(rows.size())
            .as("DB row count mismatch\nSQL: %s\nExpected=%d, Actual=%d",
                sql, expected, rows.size())
            .isEqualTo(expected);
        return this;
    }

    public DbValidator rowCountAtLeast(int minimum) {
        loadIfNeeded();
        soft.assertThat(rows.size())
            .as("DB should have at least %d rows\nSQL: %s", minimum, sql)
            .isGreaterThanOrEqualTo(minimum);
        return this;
    }

    // ── Column value assertions (on first row) ────────────────────────────────

    /**
     * Assert a column in the first row equals the expected value.
     * Column name is case-insensitive (internally lowercased).
     */
    public DbValidator columnEquals(String column, Object expected) {
        loadIfNeeded();
        if (rows.isEmpty()) {
            soft.assertThat(false)
                .as("Cannot assert column '%s' — query returned no rows\nSQL: %s", column, sql)
                .isTrue();
            return this;
        }
        Object actual = rows.get(0).get(column.toLowerCase());
        soft.assertThat(String.valueOf(actual))
            .as("Column '%s' mismatch\nSQL: %s\nExpected='%s', Actual='%s'",
                column, sql, expected, actual)
            .isEqualTo(String.valueOf(expected));
        return this;
    }

    /** Assert column in the first row is not null and not blank. */
    public DbValidator columnNotBlank(String column) {
        loadIfNeeded();
        if (rows.isEmpty()) {
            soft.assertThat(false)
                .as("Cannot assert column '%s' — no rows", column).isTrue();
            return this;
        }
        Object value = rows.get(0).get(column.toLowerCase());
        soft.assertThat(value)
            .as("Column '%s' should not be null\nSQL: %s", column, sql)
            .isNotNull();
        soft.assertThat(String.valueOf(value).trim())
            .as("Column '%s' should not be blank\nSQL: %s", column, sql)
            .isNotBlank();
        return this;
    }

    /** Assert a column value in the first row contains a substring. */
    public DbValidator columnContains(String column, String substring) {
        loadIfNeeded();
        if (rows.isEmpty()) {
            soft.assertThat(false).as("No rows returned for column check").isTrue();
            return this;
        }
        String actual = String.valueOf(rows.get(0).get(column.toLowerCase()));
        soft.assertThat(actual)
            .as("Column '%s' should contain '%s'\nSQL: %s", column, substring, sql)
            .contains(substring);
        return this;
    }

    // ── Column assertion on a specific row index ──────────────────────────────

    public DbValidator columnEqualsOnRow(int rowIndex, String column, Object expected) {
        loadIfNeeded();
        soft.assertThat(rows.size()).as("Row index %d out of range", rowIndex)
            .isGreaterThan(rowIndex);
        Object actual = rows.get(rowIndex).get(column.toLowerCase());
        soft.assertThat(String.valueOf(actual))
            .as("Row[%d] Column '%s' mismatch", rowIndex, column)
            .isEqualTo(String.valueOf(expected));
        return this;
    }

    // ── Scalar (COUNT, SUM, MAX, single-value) ────────────────────────────────

    /**
     * Assert a scalar query returns the expected value.
     * The SQL should return exactly one column in one row.
     *
     * Usage:
     *   .forQuery("SELECT COUNT(*) FROM users WHERE email=?", email)
     *   .scalarEquals(1L)
     */
    public DbValidator scalarEquals(Object expected) {
        Object actual = dbClient.queryScalar(sql, params);
        soft.assertThat(String.valueOf(actual))
            .as("Scalar value mismatch\nSQL: %s\nExpected='%s', Actual='%s'",
                sql, expected, actual)
            .isEqualTo(String.valueOf(expected));
        return this;
    }

    public DbValidator scalarGreaterThan(long minimum) {
        Object raw = dbClient.queryScalar(sql, params);
        long actual = raw != null ? Long.parseLong(raw.toString()) : 0L;
        soft.assertThat(actual)
            .as("Scalar should be > %d\nSQL: %s", minimum, sql)
            .isGreaterThan(minimum);
        return this;
    }

    // ── Null / not-null ───────────────────────────────────────────────────────

    /** Assert a specific column in the first row is NULL in the DB. */
    public DbValidator columnIsNull(String column) {
        loadIfNeeded();
        if (!rows.isEmpty()) {
            Object val = rows.get(0).get(column.toLowerCase());
            soft.assertThat(val)
                .as("Column '%s' should be NULL\nSQL: %s", column, sql)
                .isNull();
        }
        return this;
    }

    /** Assert a column is NOT NULL in the first row. */
    public DbValidator columnIsNotNull(String column) {
        loadIfNeeded();
        if (!rows.isEmpty()) {
            Object val = rows.get(0).get(column.toLowerCase());
            soft.assertThat(val)
                .as("Column '%s' should NOT be NULL\nSQL: %s", column, sql)
                .isNotNull();
        }
        return this;
    }

    // ── Cross-column compare (within same row) ────────────────────────────────

    /** Assert two columns in the first row have the same value (e.g. created_at == updated_at). */
    public DbValidator columnsAreEqual(String col1, String col2) {
        loadIfNeeded();
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            soft.assertThat(String.valueOf(row.get(col1.toLowerCase())))
                .as("Columns '%s' and '%s' should be equal", col1, col2)
                .isEqualTo(String.valueOf(row.get(col2.toLowerCase())));
        }
        return this;
    }

    // ── Raw access (for custom assertions in test) ────────────────────────────

    /** Returns the raw result list for custom assertions. */
    public List<Map<String, Object>> getRows() {
        loadIfNeeded();
        return rows;
    }

    public Optional<Map<String, Object>> getFirstRow() {
        loadIfNeeded();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // ── Terminal ──────────────────────────────────────────────────────────────

    /** Runs all accumulated soft assertions. Call at the end of the chain. */
    public void validate() {
        soft.assertAll();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadIfNeeded() {
        if (!resultLoaded) {
            if (sql == null) {
                throw new IllegalStateException(
                        "Call forQuery(sql, params...) before making assertions.");
            }
            log.debug("[{}] Loading results for: {}", dbClient.getDbType(), sql);
            rows = dbClient.query(sql, new GenericRowMapper(), params);
            resultLoaded = true;
            log.debug("[{}] Loaded {} row(s)", dbClient.getDbType(), rows.size());
        }
    }
}
