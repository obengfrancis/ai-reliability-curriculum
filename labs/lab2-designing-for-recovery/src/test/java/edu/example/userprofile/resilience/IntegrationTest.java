package edu.example.userprofile.resilience;

import edu.example.userprofile.UserProfileService;
import edu.example.userprofile.support.ControllableFakeServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests — verify that Retry and CircuitBreaker work
 * correctly when wrapping real calls to {@link UserProfileService}
 * against a controllable HTTP server.
 *
 * <p>These tests do NOT require students to refactor
 * {@code UserProfileService} itself; the patterns are applied
 * <em>around</em> the service calls at the call site, matching the
 * composition example in {@code pattern_reference.md}. A deeper
 * refactor (moving the patterns inside the service) is the natural
 * extension into Lab 3.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class IntegrationTest {

    private ControllableFakeServer server;
    private UserProfileService service;
    private Path cacheDir;

    @BeforeEach
    void setUp() throws IOException {
        server = ControllableFakeServer.start();
        cacheDir = Files.createTempDirectory("integration-cache-");
        service = new UserProfileService(server.baseUrl(), cacheDir);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    @DisplayName("Retry around UserProfileService recovers from transient 503s")
    void retryRecoversFromTransient503s() throws Exception {
        // Server fails first 2 requests with 503, then succeeds.
        server.failNext(2, 503).thenSucceed();

        Map<String, Object> profile = Retry.withBackoff(
                () -> service.fetchUserProfile("u-101"),
                3,
                Duration.ofMillis(10));

        assertNotNull(profile, "After retry recovery, the profile should be returned.");
        assertEquals("u-101", profile.get("id"),
                "Expected the recovered profile to have id=u-101; got " + profile.get("id"));
        assertEquals(3, server.requestCount(),
                "Expected exactly 3 HTTP requests (2 failures + 1 success); "
                + "got " + server.requestCount() + ". "
                + "The retry helper should have re-attempted after each transient failure.");
    }

    @Test
    @DisplayName("CircuitBreaker trips after sustained UserProfileService failures")
    void circuitBreakerTripsAfterSustainedFailures() throws Exception {
        // Every request fails with 503 for the lifetime of the test.
        server.failNext(100, 503);

        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofMillis(50));

        // Three failures should trip the breaker.
        for (int i = 0; i < 3; i++) {
            assertThrows(IOException.class,
                    () -> breaker.call(() -> service.fetchUserProfile("u-101")));
        }
        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState(),
                "Breaker should be OPEN after 3 consecutive IOExceptions from UserProfileService.");

        // Fourth call should be rejected without hitting the server.
        int requestsBefore = server.requestCount();
        assertThrows(CircuitBreakerOpenException.class,
                () -> breaker.call(() -> service.fetchUserProfile("u-101")),
                "An OPEN breaker should fail fast without calling UserProfileService.");

        assertEquals(requestsBefore, server.requestCount(),
                "The fourth call should NOT have reached the server. "
                + "Request count went from " + requestsBefore + " to " + server.requestCount() + ". "
                + "Verify that CircuitBreaker.call() throws BEFORE invoking the operation when OPEN.");
    }

    @Test
    @DisplayName("Composed: breaker (outer) + retry (inner) recovers from transient bursts")
    void composedBreakerWrapsRetry() throws Exception {
        // First 2 fail, third succeeds. Retry should handle this; breaker sees one success.
        server.failNext(2, 503).thenSucceed();

        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofMillis(50));

        // Outer = breaker, Inner = retry. This is the correct composition order.
        Map<String, Object> profile = breaker.call(() ->
                Retry.withBackoff(
                        () -> service.fetchUserProfile("u-101"),
                        3,
                        Duration.ofMillis(10)));

        assertNotNull(profile, "Composed retry-inside-breaker should recover from transient burst.");
        assertEquals(CircuitBreaker.State.CLOSED, breaker.currentState(),
                "After a successful operation (even one that required retries internally), "
                + "the breaker should be CLOSED. The retry success counted as one logical success.");
    }

    @Test
    @DisplayName("Composed: exhausted retry counts as ONE failure to the breaker")
    void exhaustedRetryCountsAsOneFailureToBreaker() throws Exception {
        // Server always fails — retry will exhaust every time.
        server.failNext(100, 503);

        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofMillis(50));

        // Each call: retry runs 3 attempts, all fail, then throws — this is ONE failure to breaker.
        // After 3 such calls, the breaker should be OPEN.
        for (int i = 0; i < 3; i++) {
            assertThrows(IOException.class, () ->
                    breaker.call(() ->
                            Retry.withBackoff(
                                    () -> service.fetchUserProfile("u-101"),
                                    3,
                                    Duration.ofMillis(5))),
                    "Each retry-exhausted call should throw IOException to the breaker.");
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.currentState(),
                "After 3 logical failures (each an exhausted retry), the breaker should be OPEN. "
                + "If it is still CLOSED, your breaker may be counting each retry attempt as a "
                + "separate failure — which would have tripped it after just 1 logical operation. "
                + "Verify the composition order: breaker.call(() -> Retry.withBackoff(...)) "
                + "so that the retry runs INSIDE the breaker's call(), making one breaker.call() "
                + "increment the failure counter by exactly one.");

        // Total HTTP requests should be 9 (3 logical ops × 3 retry attempts each).
        assertEquals(9, server.requestCount(),
                "Expected 9 total HTTP requests (3 logical ops × 3 retry attempts each); "
                + "got " + server.requestCount() + ".");
    }
}
