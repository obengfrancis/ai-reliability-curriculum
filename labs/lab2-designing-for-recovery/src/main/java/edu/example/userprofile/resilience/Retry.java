package edu.example.userprofile.resilience;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Retry-with-exponential-backoff helper. Lab 2 students implement the
 * body of {@link #withBackoff(Callable, int, Duration)}; the rest of
 * the project (including the test harness) expects this signature.
 *
 * <p>See {@code pattern_reference.md} for the algorithm, the jitter
 * formula, and pitfalls to avoid.
 */
public final class Retry {

    private Retry() {
        // utility class — no instances
    }

    /**
     * Execute {@code operation}, retrying transient failures with
     * exponential backoff.
     *
     * <p><b>Required behaviour:</b>
     * <ul>
     *   <li>Call {@code operation.call()} up to {@code maxAttempts}
     *       times. The first call is attempt 1, not attempt 0.</li>
     *   <li>If a call succeeds, return its result immediately.</li>
     *   <li>If a call throws a <em>transient</em> exception
     *       (see {@link #isTransient(Exception)}), wait and retry,
     *       up to the attempt cap.</li>
     *   <li>If a call throws a <em>permanent</em> exception, re-throw
     *       it immediately — do not retry.</li>
     *   <li>{@link CircuitBreakerOpenException} is permanent — never
     *       retry it, even though it is a RuntimeException.</li>
     *   <li>Wait time between attempts must use full jitter:
     *       {@code random(0, baseDelay * 2^(attempt - 1))}.</li>
     *   <li>If {@link Thread#sleep(long)} is interrupted during a
     *       wait, restore the interrupted flag and propagate the
     *       {@link InterruptedException}.</li>
     *   <li>After {@code maxAttempts} failed attempts, re-throw the
     *       most recent exception.</li>
     * </ul>
     *
     * @param operation   the operation to attempt
     * @param maxAttempts total number of attempts including the first
     *                    call (must be {@code >= 1})
     * @param baseDelay   wait time for the first backoff
     * @return the result of the first successful attempt
     * @throws Exception the most recent exception thrown by
     *                   {@code operation}, or InterruptedException
     *                   if waiting was interrupted
     */
    public static <T> T withBackoff(
            Callable<T> operation,
            int maxAttempts,
            Duration baseDelay) throws Exception {

        // TODO Lab 2 (Retry): implement the retry loop here.
        //
        // Algorithm sketch (full version in pattern_reference.md):
        //   for attempt in 1..maxAttempts:
        //       try:
        //           return operation.call()
        //       except e:
        //           if not isTransient(e) or attempt == maxAttempts:
        //               throw e
        //           sleep( random(0, baseDelay * 2^(attempt - 1)) )
        //
        // Reminders:
        //   - Use ThreadLocalRandom.current().nextLong(...) for jitter.
        //   - Wrap Thread.sleep in try/catch; restore the interrupted
        //     flag before propagating InterruptedException.
        //   - Treat CircuitBreakerOpenException as permanent (see
        //     isTransient below — it already handles this correctly).

        throw new UnsupportedOperationException(
                "Retry.withBackoff: not yet implemented. See TODO in Retry.java.");
    }

    /**
     * Decide whether an exception should trigger a retry.
     *
     * <p>Default classification:
     * <ul>
     *   <li>{@link CircuitBreakerOpenException} → <b>permanent</b>
     *       (do not retry; the breaker is signalling "do not call").</li>
     *   <li>{@link java.io.IOException} → <b>transient</b>
     *       (network blips, timeouts, transient 5xx — retry).</li>
     *   <li>Everything else → <b>permanent</b>
     *       (programming errors, validation failures, etc.).</li>
     * </ul>
     *
     * <p>Real production code may classify more carefully (e.g., distinct
     * handling for HTTP 4xx vs 5xx, distinct handling for SQL transient
     * vs constraint failures). For Lab 2, this default is sufficient.
     */
    static boolean isTransient(Exception e) {
        if (e instanceof CircuitBreakerOpenException) {
            return false;
        }
        return e instanceof java.io.IOException;
    }
}
