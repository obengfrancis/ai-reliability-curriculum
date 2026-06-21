package edu.example.userprofile.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Core required tests for {@link Retry#withBackoff(Callable, int, Duration)}.
 *
 * <p>These tests use millisecond-scale base delays so the full suite
 * runs in well under one second. The behaviour being verified is the
 * retry logic, not real-time wait calibration.
 *
 * <p>Every test is bounded by an explicit {@link Timeout} so a hung
 * implementation does not hang the whole suite.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class RetryTest {

    private static final Duration BASE = Duration.ofMillis(10);

    @Test
    @DisplayName("Returns immediately on first-attempt success")
    void returnsImmediatelyOnSuccess() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            return "OK";
        };

        String result = Retry.withBackoff(operation, 3, BASE);

        assertEquals("OK", result,
                "Retry should return the operation's value on success.");
        assertEquals(1, attempts.get(),
                "An operation that succeeds on the first try should be called exactly once. "
                + "Your retry helper appears to be retrying successful operations. "
                + "Check the loop's success path in Retry.java.");
    }

    @Test
    @DisplayName("Retries transient failures until success")
    void retriesTransientFailures() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> operation = () -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw new IOException("transient blip");
            }
            return "OK";
        };

        String result = Retry.withBackoff(operation, 5, BASE);

        assertEquals("OK", result,
                "After 2 transient failures, the third attempt should succeed and its value returned.");
        assertEquals(3, attempts.get(),
                "Expected exactly 3 attempts (2 fails then 1 success); your helper called "
                + "the operation " + attempts.get() + " times. "
                + "Check that successful attempts exit the retry loop.");
    }

    @Test
    @DisplayName("Does not retry permanent failures")
    void doesNotRetryPermanentFailures() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("malformed input");
        };

        assertThrows(IllegalArgumentException.class,
                () -> Retry.withBackoff(operation, 5, BASE),
                "Permanent exceptions should propagate immediately, not be retried.");

        assertEquals(1, attempts.get(),
                "Expected exactly 1 attempt for a permanent (non-IOException) failure; "
                + "your helper made " + attempts.get() + " attempts. "
                + "Are you treating all exceptions as transient? "
                + "Only IOException should retry by default — see Retry.isTransient().");
    }

    @Test
    @DisplayName("Stops after maxAttempts and propagates the final exception")
    void stopsAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger(0);
        IOException finalError = new IOException("attempt-specific marker");

        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            // Throw a fresh exception each time so we can verify the LAST one is propagated
            throw new IOException("attempt " + attempts.get());
        };

        IOException thrown = assertThrows(IOException.class,
                () -> Retry.withBackoff(operation, 3, BASE),
                "After maxAttempts transient failures, the most recent exception should be re-thrown.");

        assertEquals(3, attempts.get(),
                "Expected exactly maxAttempts=3 attempts; your helper made "
                + attempts.get() + ". "
                + "Check the loop bound and that the final failed attempt re-throws "
                + "rather than waiting again.");

        assertTrue(thrown.getMessage().contains("attempt 3"),
                "The exception propagated should be from the FINAL attempt (attempt 3), "
                + "but got: \"" + thrown.getMessage() + "\". "
                + "Are you saving and re-throwing the first exception instead of the last?");
    }

    @Test
    @DisplayName("Does not retry CircuitBreakerOpenException")
    void doesNotRetryCircuitBreakerOpenException() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            throw new CircuitBreakerOpenException();
        };

        assertThrows(CircuitBreakerOpenException.class,
                () -> Retry.withBackoff(operation, 5, BASE),
                "CircuitBreakerOpenException should propagate immediately without retrying.");

        assertEquals(1, attempts.get(),
                "When the circuit breaker is open, retrying is exactly wrong — "
                + "the breaker is signalling 'do not call'. "
                + "Your helper made " + attempts.get() + " attempts; expected 1. "
                + "Check that isTransient() returns false for CircuitBreakerOpenException.");
    }

    @Test
    @DisplayName("Restores interrupted flag and propagates InterruptedException")
    void restoresInterruptedFlagOnInterrupt() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            throw new IOException("transient");
        };

        // Use a longer base delay so the interrupt lands during sleep, not between attempts.
        Duration longerBase = Duration.ofMillis(500);

        Thread current = Thread.currentThread();
        // Schedule an interrupt 50ms from now, during the first backoff wait.
        Thread interrupter = new Thread(() -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            current.interrupt();
        });
        interrupter.start();

        try {
            assertThrows(InterruptedException.class,
                    () -> Retry.withBackoff(operation, 5, longerBase),
                    "When the calling thread is interrupted during backoff sleep, "
                    + "InterruptedException should propagate.");
        } finally {
            interrupter.join();
        }

        assertTrue(Thread.interrupted(),  // clears the flag for subsequent tests
                "After propagating InterruptedException, the thread's interrupted flag "
                + "should still be set (Thread.sleep clears it on throw, so you must "
                + "restore it via Thread.currentThread().interrupt() before re-throwing). "
                + "See the InterruptedException pitfall in pattern_reference.md.");
    }
}
