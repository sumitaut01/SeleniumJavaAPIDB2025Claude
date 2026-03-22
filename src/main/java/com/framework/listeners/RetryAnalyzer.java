package com.framework.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test up to MAX_RETRY_COUNT times before marking it FAILED.
 *
 * Usage — apply per test:
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *
 * Usage — apply globally via RetryTransformer (preferred for large suites).
 *
 * What counts as a retry:
 *  - Any test that throws an AssertionError or Exception (i.e. FAILED status)
 *  - SKIPPED tests are NOT retried
 *
 * Retry count is controlled by system property:
 *   -Dretry.count=2   (default = 1, i.e. one retry before final FAIL)
 *
 * The retry count resets per test method — a single method can retry up to
 * MAX_RETRY_COUNT times regardless of how many other tests ran.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);

    private static final int MAX_RETRY_COUNT =
            Integer.parseInt(System.getProperty("retry.count", "1"));

    /** Per-instance counter — ThreadLocal not needed; TestNG creates one instance per test. */
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            log.warn("RETRY [{}/{}] → {}.{}",
                    retryCount,
                    MAX_RETRY_COUNT,
                    result.getTestClass().getName(),
                    result.getMethod().getMethodName());
            return true;   // tell TestNG to re-run this test
        }
        return false;       // exhausted retries — mark as FAILED
    }

    /** Returns how many times this analyzer has retried (useful for reporting). */
    public int getRetryCount() {
        return retryCount;
    }
}
