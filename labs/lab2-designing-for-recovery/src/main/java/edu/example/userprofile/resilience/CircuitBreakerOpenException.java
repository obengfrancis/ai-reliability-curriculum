package edu.example.userprofile.resilience;

/**
 * Thrown by {@link CircuitBreaker#call(java.util.concurrent.Callable)}
 * when the breaker is in the OPEN state and the call is rejected
 * without ever reaching the wrapped operation.
 *
 * <p>This is a {@link RuntimeException} so callers do not need to
 * declare it; downstream code that wants to handle "the breaker is
 * open" specifically can catch it directly. The {@link Retry} helper
 * <em>must</em> treat this exception as permanent and not retry it.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException() {
        super("circuit breaker is open; downstream is currently unavailable");
    }
}
