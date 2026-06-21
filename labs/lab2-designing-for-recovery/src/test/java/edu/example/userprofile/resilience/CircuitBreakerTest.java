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
 * Core required tests for {@link CircuitBreaker}.
 *
 * <p>These tests use millisecond-scale open durations so the full
 * suite runs in well under one second. The behaviour being verified
 * is the state machine, not real-time calibration.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class CircuitBreakerTest {

    private static final Duration SHORT_OPEN = Duration.ofMillis(50);

    @Test
    @DisplayName("CLOSED breaker passes successful calls through")
    void closedBreakerPassesSuccessThrough() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(3, SHORT_OPEN);

        String result = breaker.call(() -> "OK");

        assertEquals("OK", result,
                "A CLOSED breaker should return the operation's value unchanged.");
        assertEquals(CircuitBreaker.State.CLOSED, breaker.currentState(),
                "Successful calls should leave the breaker CLOSED.");
    }

    @Test
    @DisplayName("Successful calls reset the failure counter")
    void successResetsFailureCounter() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(3, SHORT_OPEN);
        Callable<String> okOp = () -> "OK";
        Callable<String> failOp = () -> { throw new IOException("transient"); };

        // 2 failures (just under threshold of 3)
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertEquals(CircuitBreaker.State.CLOSED, breaker.currentState(),
                "Breaker should still be CLOSED after 2 failures with threshold 3.");

        // Now succeed
        breaker.call(okOp);

        // Now 2 more failures — should still be CLOSED if counter was reset
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));

        assertEquals(CircuitBreaker.State.CLOSED, breaker.currentState(),
                "After a successful call, the failure counter should reset to 0. "
                + "Two more failures should not trip the breaker (only 2/3). "
                + "If the breaker is OPEN here, the counter is accumulating across successes — "
                + "ensure you reset consecutiveFailures = 0 in the success path.");
    }

    @Test
    @DisplayName("Reaching the failure threshold opens the breaker")
    void thresholdOpensBreaker() {
        CircuitBreaker breaker = new CircuitBreaker(3, SHORT_OPEN);
        Callable<String> failOp = () -> { throw new IOException("downstream is down"); };

        // 3 consecutive failures (== threshold)
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));

        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState(),
                "After reaching the failure threshold (3 consecutive failures), "
                + "the breaker should transition to OPEN. "
                + "If it is still CLOSED, check that you transition state in the "
                + "catch path when consecutiveFailures >= failureThreshold.");
    }

    @Test
    @DisplayName("OPEN breaker rejects calls with CircuitBreakerOpenException")
    void openBreakerRejectsCalls() {
        CircuitBreaker breaker = new CircuitBreaker(2, SHORT_OPEN);
        Callable<String> failOp = () -> { throw new IOException("down"); };

        // Trip the breaker
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState());

        // Now a call should be rejected with CircuitBreakerOpenException
        AtomicInteger operationInvocations = new AtomicInteger(0);
        Callable<String> trackedOp = () -> {
            operationInvocations.incrementAndGet();
            return "should not get here";
        };

        assertThrows(CircuitBreakerOpenException.class,
                () -> breaker.call(trackedOp),
                "An OPEN breaker should reject calls without invoking the operation.");

        assertEquals(0, operationInvocations.get(),
                "The operation must NOT be called while the breaker is OPEN. "
                + "Your implementation invoked it " + operationInvocations.get() + " time(s). "
                + "Check that you throw CircuitBreakerOpenException BEFORE calling "
                + "operation.call() when state == OPEN.");
    }

    @Test
    @DisplayName("OPEN breaker transitions to HALF_OPEN after openDuration elapses")
    void openTransitionsToHalfOpenAfterDuration() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(2, SHORT_OPEN);
        Callable<String> failOp = () -> { throw new IOException("down"); };

        // Trip the breaker
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState());

        // Wait longer than openDuration
        Thread.sleep(SHORT_OPEN.toMillis() + 20);

        // The next call should be admitted (transitioning to HALF_OPEN internally).
        // We use a successful call so we can observe the post-call state.
        String result = breaker.call(() -> "OK");

        assertEquals("OK", result,
                "After openDuration elapses, the breaker should admit the next call "
                + "as a probe. Your breaker still rejected the call.");
    }

    @Test
    @DisplayName("Successful probe in HALF_OPEN closes the breaker")
    void successfulProbeClosesBreaker() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(2, SHORT_OPEN);
        Callable<String> failOp = () -> { throw new IOException("down"); };

        // Trip then wait for half-open admission window
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));
        Thread.sleep(SHORT_OPEN.toMillis() + 20);

        // Probe with a success
        breaker.call(() -> "OK");

        assertEquals(CircuitBreaker.State.CLOSED, breaker.currentState(),
                "After a successful probe in HALF_OPEN, the breaker should transition to CLOSED. "
                + "If it is still HALF_OPEN, check that you transition to CLOSED in the "
                + "success path (after operation.call() returns).");
    }

    @Test
    @DisplayName("Failed probe in HALF_OPEN re-opens the breaker and resets the timer")
    void failedProbeReopensBreaker() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(2, SHORT_OPEN);
        Callable<String> failOp = () -> { throw new IOException("still down"); };

        // Trip
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertThrows(IOException.class, () -> breaker.call(failOp));

        // Wait for half-open admission
        Thread.sleep(SHORT_OPEN.toMillis() + 20);

        // Probe fails — breaker should re-open
        assertThrows(IOException.class, () -> breaker.call(failOp));
        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState(),
                "A failed half-open probe should transition the breaker back to OPEN, "
                + "not leave it in HALF_OPEN.");

        // Immediately try again — should be rejected because the timer reset.
        // (If the timer didn't reset, this call might be admitted as another probe.)
        AtomicInteger operationInvocations = new AtomicInteger(0);
        Callable<String> trackedOp = () -> {
            operationInvocations.incrementAndGet();
            return "should not get here";
        };

        assertThrows(CircuitBreakerOpenException.class,
                () -> breaker.call(trackedOp),
                "Immediately after a failed half-open probe, the next call should be rejected "
                + "because openedAt was reset to 'now'.");

        assertEquals(0, operationInvocations.get(),
                "The operation must not have been invoked because the openedAt timer "
                + "should have been reset after the failed probe. "
                + "Check that you update openedAt = now when transitioning OPEN <- HALF_OPEN.");
    }
}
