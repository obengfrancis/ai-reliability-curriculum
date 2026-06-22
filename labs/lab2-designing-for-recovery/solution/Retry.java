package edu.example.userprofile.resilience;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * INSTRUCTOR REFERENCE SOLUTION for {@link Retry#withBackoff(Callable, int, Duration)}.
 *
 * <p><b>Do not share this file with students.</b> It is the answer key
 * used to validate that a correct implementation makes all 17 required
 * Lab 2 tests pass.
 *
 * <p>This file is a drop-in replacement for the starter {@code Retry.java}.
 * Swap it in temporarily to verify the harness, then swap the starter
 * back before releasing the lab.
 */
public final class Retry {

    private Retry() {
        // utility class
    }

    public static <T> T withBackoff(
            Callable<T> operation,
            int maxAttempts,
            Duration baseDelay) throws Exception {

        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastError = e;
                // Permanent failure, or out of attempts: propagate immediately.
                if (!isTransient(e) || attempt == maxAttempts) {
                    throw e;
                }
                // Full jitter: random wait in [0, baseDelay * 2^(attempt-1)].
                long ceilingMs = baseDelay.toMillis() << (attempt - 1);
                long waitMs = ThreadLocalRandom.current().nextLong(ceilingMs + 1);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    // Restore the interrupted flag (Thread.sleep clears it on
                    // throw) so callers see both the exception and the flag.
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
        }
        // Loop terminates only via return or throw; this line is unreachable
        // in practice. Including it satisfies the compiler.
        throw (lastError != null) ? lastError : new IllegalStateException("unreachable");
    }

    static boolean isTransient(Exception e) {
        if (e instanceof CircuitBreakerOpenException) {
            return false;
        }
        return e instanceof java.io.IOException;
    }
}
