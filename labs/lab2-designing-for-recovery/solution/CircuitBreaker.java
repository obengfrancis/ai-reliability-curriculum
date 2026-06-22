package edu.example.userprofile.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

/**
 * INSTRUCTOR REFERENCE SOLUTION for {@link CircuitBreaker}.
 *
 * <p><b>Do not share this file with students.</b> It is the answer key
 * used to validate that a correct implementation makes all 17 required
 * Lab 2 tests pass.
 *
 * <p>This file is a drop-in replacement for the starter
 * {@code CircuitBreaker.java}. Swap it in temporarily to verify the
 * harness, then swap the starter back before releasing the lab.
 */
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private Instant openedAt = null;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public synchronized <T> T call(Callable<T> operation) throws Exception {
        // If OPEN, check whether enough time has passed to attempt a probe.
        if (state == State.OPEN) {
            if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) >= 0) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException();
            }
        }

        try {
            T result = operation.call();
            // Success: reset the counter and (re-)enter CLOSED.
            consecutiveFailures = 0;
            state = State.CLOSED;
            return result;
        } catch (Exception e) {
            consecutiveFailures++;
            // A failure in HALF_OPEN reopens the breaker;
            // hitting the threshold in CLOSED also opens it.
            if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
                state = State.OPEN;
                openedAt = Instant.now();
            }
            throw e;
        }
    }

    public synchronized State currentState() {
        return state;
    }
}
