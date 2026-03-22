package com.framework.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages ExtentReports lifecycle.
 * - ExtentReports is a singleton (one per run).
 * - ExtentTest is thread-local (one per test thread).
 *
 * Flush must be called in the TestNG suite-level @AfterSuite listener.
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    private ExtentReportManager() { /* static utility */ }

    // ── Initialise (called once per suite) ──────────────────────────────────
    public static synchronized void initReports() {
        if (extentReports == null) {
            // ensure directory exists
            new File(FrameworkConstants.REPORTS_DIR).mkdirs();
            new File(FrameworkConstants.SCREENSHOT_DIR).mkdirs();

            String timestamp  = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportPath = FrameworkConstants.REPORTS_DIR
                    + "ExtentReport_" + timestamp + ".html";

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("Automation Report");
            spark.config().setReportName("Selenium TestNG Framework");
            spark.config().setEncoding("UTF-8");

            extentReports = new ExtentReports();
            extentReports.attachReporter(spark);

            ConfigManager cfg = ConfigManager.getInstance();
            extentReports.setSystemInfo("Environment",  cfg.getEnvironment().name());
            extentReports.setSystemInfo("Country",      cfg.getCountry().name());
            extentReports.setSystemInfo("Browser",      cfg.getBrowser().name());
            extentReports.setSystemInfo("Execution",    cfg.getExecutionType().name());
            extentReports.setSystemInfo("Base URL",     cfg.getBaseUrl());
            extentReports.setSystemInfo("Executed By",  System.getProperty("user.name"));

            log.info("ExtentReports initialized → {}", reportPath);
        }
    }

    // ── Create test node ─────────────────────────────────────────────────────
    public static synchronized ExtentTest createTest(String testName, String description) {
        ExtentTest test = extentReports.createTest(testName, description);
        extentTestThreadLocal.set(test);
        return test;
    }

    // ── Get current thread's test node ──────────────────────────────────────
    public static ExtentTest getTest() {
        return extentTestThreadLocal.get();
    }

    // ── Remove thread-local entry ────────────────────────────────────────────
    public static void removeTest() {
        extentTestThreadLocal.remove();
    }

    // ── Flush (called once per suite) ────────────────────────────────────────
    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
            log.info("ExtentReports flushed.");
        }
    }
}
