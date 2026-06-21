package edu.example.userprofile.resilience;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Circuit breaker that wraps an operation and refuses to call it
 * while a downstream is judged to be failing.
 *
 * <p>Lab 2 students implement the body of
 * {@link #call(Callable)} and {@link #currentState()}; the test
 * harness expects these signatures exactly.
 *
 * <p>See {@code pattern_reference.md} for the state machine
 * (CLOSED → OPEN → HALF_OPEN → CLOSED) and threshold/probe
 * decisions.
 */
public class CircuitBreaker {

    /** State of the breaker. The test suite reads this via {@link #currentState()}. */
    public enum State {
        /** Calls pass through. Normal operation. */
        CLOSED,
        /** Calls are rejected without reaching the operation. */
        OPEN,
        /** Probing — the next call is admitted to test whether the downstream has recovered. */
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;

    // TODO Lab 2 (CircuitBreaker): add state fields here.
    //
    // You will need at least:
    //   - State state              (current state)
    //   - int consecutiveFailures  (count toward the threshold)
    //   - Instant openedAt         (when the breaker entered OPEN)

    /**
     * Construct a circuit breaker.
     *
     * @param failureThreshold number of consecutive failures that opens
     *                         the breaker (must be {@code >= 1})
     * @param openDuration     how long the breaker stays OPEN before
     *                         transitioning to HALF_OPEN
     */
    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        // TODO Lab 2: initialise state to CLOSED, consecutiveFailures to 0.
    }

    /**
     * Execute {@code operation} through the breaker.
     *
     * <p><b>Required behaviour:</b>
     * <ul>
     *   <li>If state is CLOSED, call the operation. On success, reset
     *       the failure counter. On failure, increment the counter;
     *       if the counter has reached {@code failureThreshold},
     *       transition to OPEN and record the time.</li>
     *   <li>If state is OPEN and {@code openDuration} has not yet
     *       elapsed since the breaker opened, throw
     *       {@link CircuitBreakerOpenException} without calling the
     *       operation.</li>
     *   <li>If state is OPEN and {@code openDuration} has elapsed,
     *       transition to HALF_OPEN and proceed as if the breaker
     *       were in HALF_OPEN.</li>
     *   <li>If state is HALF_OPEN, call the operation. On success,
     *       transition to CLOSED and reset the failure counter.
     *       On failure, transition back to OPEN and reset the
     *       {@code openedAt} timestamp.</li>
     *   <li>Methods that mutate state must be thread-safe; the
     *       simplest correct approach is to mark this method
     *       {@code synchronized}.</li>
     * </ul>
     *
     * @return the result of {@code operation.call()}
     * @throws CircuitBreakerOpenException if the breaker is OPEN
     * @throws Exception                   the exception thrown by the
     *                                     operation, if it failed
     */
    public synchronized <T> T call(Callable<T> operation) throws Exception {

        // TODO Lab 2 (CircuitBreaker): implement the state machine here.
        //
        // Algorithm sketch (full version in pattern_reference.md):
        //   if state == OPEN:
        //       if elapsed >= openDuration: state = HALF_OPEN
        //       else: throw new CircuitBreakerOpenException()
        //   try:
        //       result = operation.call()
        //       state = CLOSED; consecutiveFailures = 0
        //       return result
        //   except e:
        //       consecutiveFailures++
        //       if state == HALF_OPEN or consecutiveFailures >= failureThreshold:
        //           state = OPEN; openedAt = now
        //       throw e

        throw new UnsupportedOperationException(
                "CircuitBreaker.call: not yet implemented. See TODO in CircuitBreaker.java.");
    }

    /**
     * Report the breaker's current state. Used by tests to verify
     * that transitions happen at the expected moments.
     */
    public synchronized State currentState() {
        // TODO Lab 2 (CircuitBreaker): return the current state.
        throw new UnsupportedOperationException(
                "CircuitBreaker.currentState: not yet implemented. See TODO in CircuitBreaker.java.");
    }
}
