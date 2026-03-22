package com.framework.constants;

/**
 * Central place for all hard-coded paths and timing values.
 * No magic strings scattered across the codebase.
 */
public final class FrameworkConstants {

    private FrameworkConstants() { /* utility class */ }

    // ── Config file paths ────────────────────────────────────────────────────
    public static final String CONFIG_DIR         = "src/test/resources/config/";
    public static final String ENV_CONFIG_FILE    = CONFIG_DIR + "env-config.json";
    public static final String LOG4J_CONFIG_FILE  = "log4j2.xml";

    // ── Test data paths ──────────────────────────────────────────────────────
    public static final String TEST_DATA_DIR      = "src/test/resources/testdata/";
    public static final String EXCEL_DATA_FILE    = TEST_DATA_DIR + "TestData.xlsx";
    public static final String JSON_DATA_DIR      = TEST_DATA_DIR + "json/";

    // ── Reporting ────────────────────────────────────────────────────────────
    public static final String REPORTS_DIR        = "reports/";
    public static final String EXTENT_REPORT_PATH = REPORTS_DIR + "ExtentReport.html";
    public static final String SCREENSHOT_DIR     = REPORTS_DIR + "screenshots/";

    // ── Timeouts (seconds) ───────────────────────────────────────────────────
    public static final int IMPLICIT_WAIT         = 10;
    public static final int EXPLICIT_WAIT         = 20;
    public static final int PAGE_LOAD_TIMEOUT     = 60;
    public static final int SCRIPT_TIMEOUT        = 30;

    // ── Remote Grid ──────────────────────────────────────────────────────────
    public static final String REMOTE_URL_KEY     = "SELENIUM_GRID_URL";   // env variable
    public static final String DEFAULT_GRID_URL   = "http://localhost:4444/wd/hub";

    // ── Excel sheet names ────────────────────────────────────────────────────
    public static final String LOGIN_SHEET        = "LoginData";
    public static final String REGISTRATION_SHEET = "RegistrationData";

    // ── JSON data keys ───────────────────────────────────────────────────────
    public static final String LOGIN_JSON         = "login.json";
    public static final String REGISTRATION_JSON  = "registration.json";

    // ── JSON Schema paths ────────────────────────────────────────────────────
    public static final String SCHEMAS_DIR         = "src/test/resources/schemas/";
    public static final String USER_SCHEMA         = "schemas/user-response.json";
    public static final String ORDER_SCHEMA        = "schemas/order-response.json";

    // ── API defaults ─────────────────────────────────────────────────────────
    public static final int    API_RESPONSE_TIMEOUT_MS = 5000;
    public static final String CONTENT_TYPE_JSON       = "application/json";
    public static final String ADMIN_USERNAME          = "admin@qa.in";
    public static final String ADMIN_PASSWORD          = "Admin@1234";

    // ── Database ─────────────────────────────────────────────────────────────
    /** SQL LIKE pattern to bulk-delete auto-generated test users during cleanup */
    public static final String TEST_USER_EMAIL_PATTERN = "test_%@auto.%.com";

    /** Default max rows to fetch in a single DB query (prevent accidental full-table scans) */
    public static final int    DB_MAX_ROWS             = 500;

    /** Status values (mirror your DB ENUM / check constraint values) */
    public static final String STATUS_ACTIVE           = "ACTIVE";
    public static final String STATUS_INACTIVE         = "INACTIVE";
    public static final String STATUS_PENDING          = "PENDING";

    /** Audit event types (must match values in audit_log.event_type column) */
    public static final String AUDIT_USER_CREATED      = "USER_CREATED";
    public static final String AUDIT_USER_UPDATED      = "USER_UPDATED";
    public static final String AUDIT_ORDER_CREATED     = "ORDER_CREATED";
    public static final String AUDIT_ORDER_CANCELLED   = "ORDER_CANCELLED";
}
