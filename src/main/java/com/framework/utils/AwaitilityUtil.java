package com.framework.utils;

import com.framework.db.executor.DbClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Polling helpers for eventually-consistent systems.
 *
 * Use when a DB record or API state doesn't update synchronously —
 * e.g. a Kafka consumer writes to DB after the API returns 201.
 *
 * Under the hood uses Awaitility (already in pom.xml).
 *
 * Usage:
 *   // Wait for DB row to appear (e.g. async consumer writes to DB)
 *   AwaitilityUtil.waitUntilDbRowExists(pgDb,
 *       "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId);
 *
 *   // Wait for a custom condition
 *   AwaitilityUtil.waitUntil(
 *       "order status to become SHIPPED",
 *       () -> "SHIPPED".equals(
 *               pgDb.queryScalar("SELECT status FROM orders WHERE order_id=?", orderId)),
 *       Duration.ofSeconds(30));
 */
public class AwaitilityUtil {

    private static final Logger log = LogManager.getLogger(AwaitilityUtil.class);

    private static final Duration DEFAULT_TIMEOUT  = Duration.ofSeconds(30);
    private static final Duration DEFAULT_POLL     = Duration.ofSeconds(2);

    private AwaitilityUtil() {}

    // ── Generic condition polling ─────────────────────────────────────────────

    /**
     * Polls a condition until it returns true or the timeout elapses.
     *
     * @param description human-readable condition name (for failure messages)
     * @param condition   a Callable<Boolean> — return true when condition is met
     * @param timeout     max wait duration
     */
    public static void waitUntil(String description,
                                  Callable<Boolean> condition,
                                  Duration timeout) {
        log.info("Waiting up to {} for: {}", timeout, description);
        try {
            Awaitility.await(description)
                    .atMost(timeout)
                    .pollInterval(DEFAULT_POLL)
                    .until(condition);
            log.info("Condition met: {}", description);
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Timed out waiting for: " + description
                    + " (timeout=" + timeout + ")", e);
        }
    }

    /** Overload with default 30s timeout. */
    public static void waitUntil(String description, Callable<Boolean> condition) {
        waitUntil(description, condition, DEFAULT_TIMEOUT);
    }

    // ── DB-specific helpers ───────────────────────────────────────────────────

    /**
     * Waits until a COUNT(*) query returns > 0 (i.e. the row has been written).
     * Use for async consumers / event-driven architectures.
     *
     * @param client   the DbClient to query
     * @param countSql a SELECT COUNT(*) ... query
     * @param params   positional parameters
     */
    public static void waitUntilDbRowExists(DbClient client,
                                             String countSql,
                                             Object... params) {
        waitUntil(
            "DB row to appear: " + countSql,
            () -> {
                Number count = client.queryScalar(countSql, params);
                return count != null && count.longValue() > 0;
            },
            DEFAULT_TIMEOUT
        );
    }

    /**
     * Waits until a single-column scalar query returns the expected value.
     * Useful for status transitions: "wait until order.status = 'SHIPPED'".
     *
     * @param client      the DbClient to query
     * @param scalarSql   e.g. "SELECT status FROM orders WHERE order_id = ?"
     * @param expected    the value to wait for (compared via String.valueOf)
     * @param params      positional parameters
     */
    public static void waitUntilDbValueEquals(DbClient client,
                                               String scalarSql,
                                               Object expected,
                                               Object... params) {
        waitUntil(
            "DB value to become: " + expected,
            () -> {
                Object actual = client.queryScalar(scalarSql, params);
                return String.valueOf(expected).equals(String.valueOf(actual));
            },
            DEFAULT_TIMEOUT
        );
    }

    /**
     * Waits until a supplier-based value equals the expected value.
     * More general than the DB version.
     *
     * @param description human-readable name
     * @param supplier    produces the current value on each poll
     * @param expected    the value to wait for
     */
    public static <T> void waitUntilEquals(String description,
                                            Supplier<T> supplier,
                                            T expected,
                                            Duration timeout) {
        waitUntil(description, () -> {
            T actual = supplier.get();
            return expected.equals(actual);
        }, timeout);
    }
}
