package com.framework.db.queries;

/**
 * Central registry of all SQL queries used across the framework.
 *
 * Rules:
 *  - NO inline SQL in test classes or service classes
 *  - All queries parameterised with ? (JDBC standard — works for both Postgres and Oracle)
 *  - Grouped by domain (User, Order, Audit, etc.)
 *  - Column names are lowercase — GenericRowMapper lowercases them on read
 *
 * Oracle note: use ROWNUM / FETCH FIRST n ROWS for limits (no LIMIT keyword).
 * Postgres note: standard LIMIT / OFFSET.
 *
 * If you need DB-specific variants, add them as separate constants
 * with _PG or _ORA suffix and select in the service layer based on DbType.
 */
public final class DbQueries {

    private DbQueries() {}

    // ════════════════════════════════════════════════════════════════════════
    // USER QUERIES
    // ════════════════════════════════════════════════════════════════════════

    public static final class User {

        /** Check if a user exists by email */
        public static final String EXISTS_BY_EMAIL =
                "SELECT COUNT(*) FROM users WHERE email = ?";

        /** Fetch a user row by user_id */
        public static final String FIND_BY_ID =
                "SELECT user_id, email, first_name, last_name, status, country, role, " +
                "created_at, updated_at FROM users WHERE user_id = ?";

        /** Fetch a user row by email */
        public static final String FIND_BY_EMAIL =
                "SELECT user_id, email, first_name, last_name, status, country, role, " +
                "created_at, updated_at FROM users WHERE email = ?";

        /** Get the current status of a user */
        public static final String GET_STATUS =
                "SELECT status FROM users WHERE user_id = ?";

        /** Get all users for a country (used in multi-country reporting) */
        public static final String FIND_BY_COUNTRY =
                "SELECT user_id, email, status FROM users WHERE country = ? ORDER BY created_at DESC";

        /** Soft-delete check (deleted_at should be null for active users) */
        public static final String IS_NOT_DELETED =
                "SELECT COUNT(*) FROM users WHERE user_id = ? AND deleted_at IS NULL";

        /** Update user status directly in DB (for test teardown / rollback) */
        public static final String UPDATE_STATUS =
                "UPDATE users SET status = ?, updated_at = NOW() WHERE user_id = ?";

        /** Hard delete — test cleanup only */
        public static final String DELETE_BY_ID =
                "DELETE FROM users WHERE user_id = ?";

        /** Delete test-generated users (email pattern matching) — bulk cleanup */
        public static final String DELETE_TEST_USERS =
                "DELETE FROM users WHERE email LIKE ?";
    }

    // ════════════════════════════════════════════════════════════════════════
    // ORDER QUERIES
    // ════════════════════════════════════════════════════════════════════════

    public static final class Order {

        public static final String FIND_BY_ID =
                "SELECT order_id, user_id, product_id, quantity, status, currency, " +
                "total_amount, created_at FROM orders WHERE order_id = ?";

        public static final String FIND_BY_USER =
                "SELECT order_id, status, total_amount, created_at " +
                "FROM orders WHERE user_id = ? ORDER BY created_at DESC";

        public static final String COUNT_BY_USER =
                "SELECT COUNT(*) FROM orders WHERE user_id = ?";

        public static final String GET_STATUS =
                "SELECT status FROM orders WHERE order_id = ?";

        public static final String COUNT_PENDING_BY_USER =
                "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = 'PENDING'";

        /** Oracle variant — same logic, same ? binding */
        public static final String COUNT_BY_USER_ORA =
                "SELECT COUNT(*) FROM orders WHERE user_id = ?";
    }

    // ════════════════════════════════════════════════════════════════════════
    // AUDIT LOG QUERIES
    // ════════════════════════════════════════════════════════════════════════

    public static final class Audit {

        public static final String FIND_BY_ENTITY =
                "SELECT event_type, entity_id, performed_by, created_at " +
                "FROM audit_log WHERE entity_id = ? ORDER BY created_at DESC";

        public static final String COUNT_EVENTS_BY_ENTITY =
                "SELECT COUNT(*) FROM audit_log WHERE entity_id = ? AND event_type = ?";

        public static final String LATEST_EVENT =
                "SELECT event_type, entity_id, created_at " +
                "FROM audit_log WHERE entity_id = ? ORDER BY created_at DESC LIMIT 1";

        /** Oracle equivalent — no LIMIT, uses ROWNUM instead */
        public static final String LATEST_EVENT_ORA =
                "SELECT event_type, entity_id, created_at FROM audit_log " +
                "WHERE entity_id = ? AND ROWNUM = 1 ORDER BY created_at DESC";
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRICING / RULE ENGINE (X4V domain)
    // ════════════════════════════════════════════════════════════════════════

    public static final class Pricing {

        public static final String FIND_RULE_BY_ID =
                "SELECT rule_id, rule_name, power_rank, is_active, country " +
                "FROM pricing_rules WHERE rule_id = ?";

        public static final String COUNT_ACTIVE_RULES_BY_COUNTRY =
                "SELECT COUNT(*) FROM pricing_rules WHERE country = ? AND is_active = true";

        public static final String FIND_RULES_BY_COUNTRY =
                "SELECT rule_id, rule_name, power_rank FROM pricing_rules " +
                "WHERE country = ? ORDER BY power_rank ASC";
    }
}
