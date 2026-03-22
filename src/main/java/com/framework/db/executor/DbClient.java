package com.framework.db.executor;

import com.framework.db.connection.ConnectionManager;
import com.framework.db.mapper.GenericRowMapper;
import com.framework.db.mapper.RowMapper;
import com.framework.enums.DbType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core JDBC executor for the framework.
 *
 * Every public method obtains a connection from the HikariCP pool,
 * executes, and returns it — callers never manage connections directly.
 *
 * Supports:
 *  - query()        → SELECT → List<T>
 *  - queryOne()     → SELECT → Optional<T>  (asserts 0-or-1 rows)
 *  - queryScalar()  → SELECT → single value (COUNT, MAX, SUM, etc.)
 *  - execute()      → INSERT / UPDATE / DELETE → rows affected
 *  - executeBatch() → batch INSERT/UPDATE
 *  - callProc()     → stored procedure / function call
 *
 * Constructor:
 *   DbClient pgClient = new DbClient(DbType.POSTGRES);
 *   DbClient orClient = new DbClient(DbType.ORACLE);
 *
 * Both clients are pre-wired in BaseTest as pgDb and orDb.
 */
public class DbClient {

    private static final Logger log = LogManager.getLogger(DbClient.class);

    private final DbType dbType;

    public DbClient(DbType dbType) {
        this.dbType = dbType;
    }

    // ── SELECT → List<T> ─────────────────────────────────────────────────────

    /**
     * Executes a SELECT and maps every row using the provided RowMapper.
     *
     * @param sql    parameterised SQL — use ? placeholders
     * @param mapper RowMapper<T> implementation
     * @param params positional parameters for ?
     * @return list of mapped rows (empty list if no rows found)
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        log.debug("[{}] QUERY: {}", dbType, sql);
        List<T> results = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection(dbType);
             PreparedStatement ps = prepare(conn, sql, params);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapper.map(rs));
            }

        } catch (SQLException e) {
            throw new DbException("Query failed on " + dbType + ": " + sql, e);
        }

        log.debug("[{}] QUERY returned {} row(s)", dbType, results.size());
        return results;
    }

    /**
     * Convenience overload — maps to Map<String,Object> without a custom RowMapper.
     */
    public List<Map<String, Object>> query(String sql, Object... params) {
        return query(sql, new GenericRowMapper(), params);
    }

    // ── SELECT → Optional<T> (0 or 1 row) ────────────────────────────────────

    /**
     * Executes a SELECT expecting at most 1 row.
     * Returns Optional.empty() if no row found.
     * Throws DbException if more than 1 row is returned.
     */
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);

        if (rows.size() > 1) {
            throw new DbException("queryOne expected 0 or 1 row but got " + rows.size()
                    + " | SQL: " + sql);
        }
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Convenience — returns Optional<Map<String,Object>>. */
    public Optional<Map<String, Object>> queryOne(String sql, Object... params) {
        return queryOne(sql, new GenericRowMapper(), params);
    }

    // ── SELECT scalar (COUNT, MAX, single column value) ──────────────────────

    /**
     * Executes a query returning a single value (e.g. COUNT(*), MAX(id)).
     * Returns null if no rows matched.
     *
     * Usage:
     *   long count = dbClient.queryScalar("SELECT COUNT(*) FROM users WHERE email=?", email);
     */
    @SuppressWarnings("unchecked")
    public <T> T queryScalar(String sql, Object... params) {
        log.debug("[{}] SCALAR: {}", dbType, sql);

        try (Connection conn = ConnectionManager.getConnection(dbType);
             PreparedStatement ps = prepare(conn, sql, params);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return (T) rs.getObject(1);
            }
            return null;

        } catch (SQLException e) {
            throw new DbException("Scalar query failed on " + dbType + ": " + sql, e);
        }
    }

    // ── INSERT / UPDATE / DELETE ──────────────────────────────────────────────

    /**
     * Executes an INSERT, UPDATE, or DELETE.
     *
     * @return number of rows affected
     */
    public int execute(String sql, Object... params) {
        log.debug("[{}] EXECUTE: {}", dbType, sql);

        try (Connection conn = ConnectionManager.getConnection(dbType);
             PreparedStatement ps = prepare(conn, sql, params)) {

            int rows = ps.executeUpdate();
            log.debug("[{}] EXECUTE affected {} row(s)", dbType, rows);
            return rows;

        } catch (SQLException e) {
            throw new DbException("Execute failed on " + dbType + ": " + sql, e);
        }
    }

    // ── INSERT returning generated key ────────────────────────────────────────

    /**
     * Executes an INSERT and returns the generated primary key.
     * Useful for Postgres SERIAL / IDENTITY columns.
     */
    public Object executeAndReturnKey(String sql, Object... params) {
        log.debug("[{}] INSERT+KEY: {}", dbType, sql);

        try (Connection conn = ConnectionManager.getConnection(dbType);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParams(ps, params);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getObject(1);
            }
            return null;

        } catch (SQLException e) {
            throw new DbException("Insert+key failed on " + dbType + ": " + sql, e);
        }
    }

    // ── Batch execute ─────────────────────────────────────────────────────────

    /**
     * Executes the same SQL statement for a list of parameter sets in a single batch.
     *
     * @param sql        parameterised SQL
     * @param paramSets  each inner array = one row's parameters
     * @return array of row counts per batch entry
     */
    public int[] executeBatch(String sql, List<Object[]> paramSets) {
        log.debug("[{}] BATCH ({} rows): {}", dbType, paramSets.size(), sql);

        try (Connection conn = ConnectionManager.getConnection(dbType);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Object[] params : paramSets) {
                setParams(ps, params);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);

            log.debug("[{}] BATCH complete — {} statements", dbType, results.length);
            return results;

        } catch (SQLException e) {
            throw new DbException("Batch execute failed on " + dbType + ": " + sql, e);
        }
    }

    // ── Stored Procedure ──────────────────────────────────────────────────────

    /**
     * Calls a stored procedure / function.
     *
     * @param callSql  JDBC call syntax:
     *                  Postgres  → "{ call my_proc(?, ?) }"
     *                  Oracle    → "{ call my_pkg.my_proc(?, ?) }"
     * @param params   IN parameters
     * @return first OUT parameter as Object (null if procedure has no OUT param)
     */
    public Object callProcedure(String callSql, Object... params) {
        log.debug("[{}] CALL: {}", dbType, callSql);

        try (Connection conn = ConnectionManager.getConnection(dbType);
             CallableStatement cs = conn.prepareCall(callSql)) {

            setParams(cs, params);
            cs.execute();

            // Try to read OUT param at last position + 1
            try {
                return cs.getObject(params.length + 1);
            } catch (Exception ex) {
                return null; // no OUT param
            }

        } catch (SQLException e) {
            throw new DbException("Stored procedure failed on " + dbType + ": " + callSql, e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PreparedStatement prepare(Connection conn, String sql, Object[] params)
            throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        setParams(ps, params);
        return ps;
    }

    private void setParams(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public DbType getDbType() { return dbType; }

    // ── Inner exception ───────────────────────────────────────────────────────

    public static class DbException extends RuntimeException {
        public DbException(String message, Throwable cause) {
            super(message, cause);
        }
        public DbException(String message) {
            super(message);
        }
    }
}
