package com.framework.tests;

import com.framework.api.services.AuthService;
import com.framework.api.services.OrderService;
import com.framework.api.services.UserService;
import com.framework.config.ConfigManager;
import com.framework.context.TestContext;
import com.framework.db.connection.ConnectionManager;
import com.framework.db.executor.DbTestHelper;
import com.framework.db.executor.DbClient;
import com.framework.db.validator.DbValidator;
import com.framework.drivers.DriverManager;
import com.framework.enums.DbType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for ALL test types: UI-only, API-only, DB-only, and Hybrid.
 *
 * FOUR subclass patterns:
 *
 *  1. UI-only     → extend BaseTest          (mode=UI,     driver started)
 *  2. API-only    → extend BaseApiTest       (mode=API,    no browser)
 *  3. Hybrid      → extend BaseHybridTest    (mode=HYBRID, driver + context + DB)
 *  4. DB-only     → extend BaseTest directly (mode=UI default is fine;
 *                     just don't call DriverManager — or extend BaseApiTest)
 *
 * DB clients:
 *   pgDb  — PostgreSQL client (null-safe: only created if config exists)
 *   orDb  — Oracle client     (null-safe: only created if config exists)
 *
 * Usage in tests:
 *   // Direct client usage:
 *   UserDbRecord user = pgDb.queryOne(DbQueries.User.FIND_BY_ID, UserDbRecord.rowMapper(), id)
 *                           .orElseThrow();
 *
 *   // Fluent validator:
 *   dbAssert(pgDb).forQuery(DbQueries.User.GET_STATUS, userId)
 *                 .rowExists()
 *                 .columnEquals("status", "ACTIVE")
 *                 .validate();
 */
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());

    // ── API Services ──────────────────────────────────────────────────────────
    protected final AuthService  authService  = new AuthService();
    protected final UserService  userService  = new UserService();
    protected final OrderService orderService = new OrderService();

    // ── DB clients (lazily created, null if DB not configured for this env) ───
    protected DbClient     pgDb;
    protected DbClient     orDb;
    protected DbTestHelper dbHelper;

    // ── Mode ──────────────────────────────────────────────────────────────────
    protected enum TestMode { UI, API, HYBRID }

    protected TestMode getMode() { return TestMode.UI; }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        TestContext.init();

        TestMode mode = getMode();
        log.info("------ setUp | mode={} | thread={} ------",
                mode, Thread.currentThread().getName());

        if (mode == TestMode.UI || mode == TestMode.HYBRID) {
            DriverManager.initDriver();
        }

        // Init DB clients only when the config exists for this env+country
        ConfigManager cfg = ConfigManager.getInstance();
        if (cfg.hasDb(DbType.POSTGRES)) {
            pgDb = new DbClient(DbType.POSTGRES);
            log.debug("PostgreSQL client ready");
        }
        if (cfg.hasDb(DbType.ORACLE)) {
            orDb = new DbClient(DbType.ORACLE);
            log.debug("Oracle client ready");
        }

        // DbTestHelper wires both clients (either may be null — it handles that)
        dbHelper = new DbTestHelper(pgDb, orDb);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        TestMode mode = getMode();
        log.info("------ tearDown | mode={} | thread={} ------",
                mode, Thread.currentThread().getName());

        if (mode == TestMode.UI || mode == TestMode.HYBRID) {
            DriverManager.quitDriver();
        }

        TestContext.clear();
        // Note: DB connections are pooled — no per-test close needed.
        // ConnectionManager.closeAll() is called by JVM shutdown hook.
    }

    // ── DB validation shortcut ────────────────────────────────────────────────

    /**
     * Convenience: start a fluent DB assertion chain.
     *
     * Usage:
     *   dbAssert(pgDb)
     *       .forQuery("SELECT status FROM users WHERE user_id = ?", userId)
     *       .rowExists()
     *       .columnEquals("status", "ACTIVE")
     *       .validate();
     */
    protected DbValidator dbAssert(DbClient client) {
        return DbValidator.using(client);
    }

    // ── Convenience accessors ─────────────────────────────────────────────────
    protected ConfigManager config()  { return ConfigManager.getInstance(); }
    protected String baseUrl()        { return config().getBaseUrl(); }
    protected String apiUrl()         { return config().getApiUrl(); }
    protected String country()        { return config().getCountry().name(); }
    protected TestContext ctx()       { return TestContext.get(); }
}
