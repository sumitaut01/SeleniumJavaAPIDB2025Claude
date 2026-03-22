package com.framework.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.config.ConfigManager;
import com.framework.drivers.DriverManager;
import com.framework.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG ITestListener.
 * - Hooks into test lifecycle events.
 * - Marks ExtentTest as PASS / FAIL / SKIP.
 * - Captures screenshot on failure and embeds it in the report.
 *
 * Register in testng.xml:
 *   <listeners>
 *     <listener class-name="com.framework.listeners.TestListener"/>
 *   </listeners>
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    // ── Suite level ──────────────────────────────────────────────────────────
    @Override
    public void onStart(ITestContext context) {
        log.info("===== Suite starting: {} =====", context.getName());
        ExtentReportManager.initReports();
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("===== Suite finished: {} =====", context.getName());
        ExtentReportManager.flushReports();
    }

    // ── Test level ───────────────────────────────────────────────────────────
    @Override
    public void onTestStart(ITestResult result) {
        String testName   = result.getMethod().getMethodName();
        String className  = result.getTestClass().getName();
        String desc       = result.getMethod().getDescription();
        String displayDesc = (desc != null && !desc.isBlank()) ? desc : "Test: " + testName;

        log.info(">> START  [{}.{}]", className, testName);

        ConfigManager cfg = ConfigManager.getInstance();
        ExtentReportManager.createTest(testName, displayDesc)
                .assignCategory(className)
                .assignCategory("ENV:" + cfg.getEnvironment().name())
                .assignCategory("COUNTRY:" + cfg.getCountry().name());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info(">> PASS   [{}]", result.getMethod().getMethodName());
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) test.log(Status.PASS, "Test PASSED");
        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.error(">> FAIL   [{}] → {}", testName, result.getThrowable().getMessage());

        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            // Log the exception
            test.log(Status.FAIL, result.getThrowable());

            // Capture and embed screenshot
            try {
                String screenshotPath = ScreenshotUtil.captureScreenshot(testName);
                if (screenshotPath != null) {
                    test.fail("Screenshot on failure:",
                            MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                }
            } catch (Exception e) {
                log.error("Failed to capture screenshot: {}", e.getMessage());
            }
        }
        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn(">> SKIP   [{}]", result.getMethod().getMethodName());
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.log(Status.SKIP, result.getThrowable() != null
                    ? result.getThrowable().getMessage()
                    : "Test SKIPPED");
        }
        ExtentReportManager.removeTest();
    }
}
