package com.framework.drivers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Thread-safe WebDriver holder using ThreadLocal.
 * Supports parallel test execution — each thread gets its own driver instance.
 *
 * Lifecycle:
 *   @BeforeMethod → DriverManager.initDriver()
 *   test          → DriverManager.getDriver()
 *   @AfterMethod  → DriverManager.quitDriver()
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    /** One driver per thread — safe for TestNG parallel="methods" or parallel="classes" */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() { /* static utility */ }

    /** Creates and registers a new driver for the current thread. */
    public static void initDriver() {
        if (driverThreadLocal.get() != null) {
            log.warn("Driver already exists for thread {}. Quitting old instance first.",
                    Thread.currentThread().getName());
            quitDriver();
        }
        WebDriver driver = DriverFactory.createDriver();
        driverThreadLocal.set(driver);
        log.info("Driver initialized on thread: {}", Thread.currentThread().getName());
    }

    /**
     * Returns the WebDriver for the current thread.
     * Throws if initDriver() was not called first.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver is null for thread: " + Thread.currentThread().getName()
                            + ". Did you call DriverManager.initDriver()?");
        }
        return driver;
    }

    /** Quits the driver and removes it from ThreadLocal to prevent memory leaks. */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit on thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("Error while quitting driver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }
}
