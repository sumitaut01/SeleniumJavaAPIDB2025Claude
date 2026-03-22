package com.framework.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Safety net: logs a warning when a test method completes with PASS status
 * but the test class has un-flushed SoftAssertions fields.
 *
 * This catches the "I forgot to call soft.assertAll()" mistake silently
 * passing tests that should have failed.
 *
 * Note: AssertJ's SoftAssertions throw on assertAll() — this listener is a
 * belt-and-suspenders guard that scans for common soft assertion field names.
 *
 * Register in testng.xml alongside TestListener.
 */
public class SoftAssertListener implements IInvokedMethodListener {

    private static final Logger log = LogManager.getLogger(SoftAssertListener.class);

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) return;
        if (testResult.getStatus() != ITestResult.SUCCESS) return;

        // Scan test instance for any SoftAssertions fields that were never flushed
        Object instance = testResult.getInstance();
        if (instance == null) return;

        try {
            for (java.lang.reflect.Field field : instance.getClass().getDeclaredFields()) {
                if (field.getType().getSimpleName().contains("SoftAssert")) {
                    field.setAccessible(true);
                    Object softObj = field.get(instance);
                    if (softObj != null) {
                        log.warn("POSSIBLE UNFLUSHED SOFT ASSERTIONS in [{}.{}] — " +
                                 "did you forget to call assertAll()?",
                                testResult.getTestClass().getName(),
                                testResult.getMethod().getMethodName());
                    }
                }
            }
        } catch (Exception e) {
            // Non-fatal — just log and move on
            log.debug("SoftAssertListener scan error: {}", e.getMessage());
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        // nothing needed
    }
}
