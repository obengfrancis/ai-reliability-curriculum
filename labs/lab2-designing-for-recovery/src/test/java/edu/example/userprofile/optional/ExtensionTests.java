package edu.example.userprofile.optional;

import edu.example.userprofile.resilience.CircuitBreaker;
import edu.example.userprofile.resilience.Retry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Optional / extension tests. These verify more sophisticated behaviour
 * (jitter, thread safety, etc.) that is not strictly required for Lab 2
 * but that production-grade implementations should also satisfy.
 *
 * <p>Run with:
 * <pre>{@code
 *   mvn test -Dgroups=optional
 * }</pre>
 *
 * <p>These tests are tagged {@code optional} and excluded from the
 * default Maven Surefire run. They are statistical or concurrent in
 * nature and may be flaky on slow CI runners — that is by design.
 */
@Tag("optional")
@Timeout(value = 15, unit = TimeUnit.SECONDS)
class ExtensionTests {

    @Test
    @DisplayName("Retry uses jitter — multiple runs do not always produce identical wait times")
    void retryUsesJitter() throws Exception {
        // Force many failed retries and observe the wall-clock spread of total wait times.
        // If jitter is implemented, runs will vary noticeably; if not, all runs are identical.
        Set<Long> totalWaits = new HashSet<>();
        int runs = 10;

        for (int r = 0; r < runs; r++) {
            AtomicInteger attempts = new AtomicInteger(0);
            Callable<String> failOp = () -> {
                attempts.incrementAndGet();
                throw new IOException("fail");
            };

            Instant start = Instant.now();
            try {
                Retry.withBackoff(failOp, 4, Duration.ofMillis(50));
            } catch (IOException ignored) {
                // expected
            }
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            // Bucket to 10ms resolution to make the statistical test stable.
            totalWaits.add(elapsedMs / 10);
        }

        assertTrue(totalWaits.size() >= 3,
                "Expected wall-clock total wait times to vary across " + runs + " runs "
                + "(saw only " + totalWaits.size() + " distinct buckets at 10ms resolution). "
                + "Without jitter, every run would take the same total time, which is the "
                + "synchronized retry-storm problem. "
                + "Add jitter via ThreadLocalRandom.current().nextLong(...). "
                + "Note: this test is statistical and may occasionally produce a false negative "
                + "on very fast or unusually consistent hardware.");
    }

    @Test
    @DisplayName("CircuitBreaker is thread-safe under concurrent failures")
    void circuitBreakerIsThreadSafe() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(10, Duration.ofMillis(100));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger operationInvocations = new AtomicInteger(0);

        Callable<String> failOp = () -> {
            operationInvocations.incrementAndGet();
            throw new IOException("concurrent failure");
        };

        // Submit 50 concurrent calls that will all fail.
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(pool.submit(() -> {
                try {
                    breaker.call(failOp);
                } catch (Exception ignored) {
                    // Either IOException or CircuitBreakerOpenException, both expected.
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // After all 50 calls, the breaker should definitely be OPEN.
        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState(),
                "After 50 concurrent failures with threshold 10, breaker should be OPEN.");

        // The operation should have been invoked at LEAST 10 times (threshold) but
        // not catastrophically more than the failure threshold + a small race-allowed margin.
        // If state is properly synchronized, invocations stop once the breaker opens.
        // Allow up to threshold + pool size as a safety margin for benign races.
        assertTrue(operationInvocations.get() >= 10,
                "Expected at least threshold=10 invocations before the breaker opened; "
                + "saw " + operationInvocations.get() + ".");
        assertTrue(operationInvocations.get() <= 18,
                "Expected at most threshold + pool size = 18 invocations even with "
                + "benign concurrency races; saw " + operationInvocations.get() + ". "
                + "If your number is much higher, the state mutations are not properly "
                + "synchronized — many threads are racing past the OPEN-check.");
    }

    @Test
    @DisplayName("Retry on IOException can be interrupted between retries (longer scenario)")
    void retryInterruptDuringExtendedWait() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> failOp = () -> {
            attempts.incrementAndGet();
            throw new IOException("transient");
        };

        // Long base delay to make the interrupt window wide.
        Duration longBase = Duration.ofMillis(1000);

        Thread current = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            current.interrupt();
        });
        interrupter.start();

        Instant start = Instant.now();
        try {
            assertThrows(InterruptedException.class,
                    () -> Retry.withBackoff(failOp, 5, longBase));
        } finally {
            interrupter.join();
        }
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();

        // We expect to be interrupted in well under the first full backoff window.
        assertTrue(elapsedMs < 800,
                "Interrupt should propagate promptly (well under " + longBase.toMillis()
                + "ms); took " + elapsedMs + "ms. "
                + "If much longer, your Thread.sleep call is not respecting the interrupt.");

        // Reset interrupt flag for subsequent tests.
        Thread.interrupted();
    }
}
