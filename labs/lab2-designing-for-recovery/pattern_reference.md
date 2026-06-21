# Pattern Reference — Retry with Backoff and Circuit Breaker

A reference for the two fault-tolerance patterns introduced in
**Lab 2: Designing for Recovery**. Use the *Quick Reference* below
while implementing. Read the *Detailed Notes* sections to understand
the choices behind each pattern. The *Pitfalls* section names the
mistakes most commonly made on first implementation.

---

## Quick Reference

### Retry with exponential backoff

**Intent.** Recover from *transient* failures by retrying the same
operation a small number of times, with a wait between attempts that
grows exponentially.

**When to use.** Network blips, transient 5xx HTTP responses, brief
unavailability of a downstream service, intermittent database
deadlocks. Anything where "try again in a moment" is the right
response.

**When not to use.** Non-idempotent operations without idempotency
keys (a retried POST may create duplicates). Permanent failures (4xx
HTTP responses, malformed input, authentication errors — retrying does
not help). Operations whose latency budget is already tight.

**Pseudocode.**

```
for attempt in 1..maxAttempts:
    try:
        return operation()
    except Exception as e:
        if not isTransient(e) or attempt == maxAttempts:
            raise
        wait( random(0, baseDelay * 2^(attempt-1)) )
```

### Circuit breaker

**Intent.** Stop calling a downstream service that is *sustainedly*
failing. Give it room to recover and let callers fail fast instead of
piling up on a broken dependency.

**When to use.** A downstream service that has failed many times in
a row. Cascading failure scenarios where retries amplify load on an
already-overwhelmed dependency. Production systems with strict
latency SLOs where "wait, then fail" is worse than "fail
immediately."

**When not to use.** Single-call operations where there is no shared
state to track. Internal or local operations (file I/O, in-memory
computation) where failure is not driven by external load.

**States.**

```
              failures >= threshold
   CLOSED ──────────────────────────> OPEN
     ▲                                  │
     │                                  │ open-duration elapses
     │ probe                            ▼
     │ succeeds                      HALF_OPEN
     │                                  │
     └──────────────────────────────────┘
                                       probe fails
                                       (back to OPEN)
```

**Pseudocode.**

```
on call:
    if state == OPEN:
        if open-duration elapsed: state = HALF_OPEN
        else: raise CircuitBreakerOpen
    try:
        result = operation()
        if state == HALF_OPEN: state = CLOSED
        consecutiveFailures = 0
        return result
    except:
        consecutiveFailures++
        if state == HALF_OPEN or consecutiveFailures >= threshold:
            state = OPEN; openedAt = now
        raise
```

---

## Detailed Notes — Retry with Exponential Backoff

### Example (Java)

A generic helper that retries any operation expressed as a
`Callable<T>`. Failure is signalled by a thrown `Exception`; the
helper distinguishes transient failures (retry) from permanent ones
(give up).

```java
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public final class Retry {

    private Retry() {}

    public static <T> T withBackoff(
            Callable<T> operation,
            int maxAttempts,
            Duration baseDelay) throws Exception {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                if (!isTransient(e) || attempt == maxAttempts) {
                    throw e;
                }
                // Full jitter: random wait in [0, baseDelay * 2^(attempt-1)]
                long ceilingMs = baseDelay.toMillis() << (attempt - 1);
                long waitMs = ThreadLocalRandom.current().nextLong(ceilingMs + 1);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException interrupted) {
                    // Restore the interrupted flag (Thread.sleep clears it
                    // when it throws) and propagate so callers see both.
                    Thread.currentThread().interrupt();
                    throw interrupted;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private static boolean isTransient(Exception e) {
        // Conservative default. Extend with project-specific exceptions
        // (e.g., HttpException with 5xx status) as needed.
        return e instanceof java.io.IOException;
    }
}
```

Used at the call site:

```java
Map<String, Object> profile = Retry.withBackoff(
        () -> service.fetchUserProfile(userId),
        3,                          // maxAttempts
        Duration.ofSeconds(1));     // baseDelay
```

### Jitter — why pure exponential backoff is not enough

Without jitter, every client that fails at the same moment will retry
at the same moment. A downstream service that is recovering from a
brief outage may be hit by a synchronized "thundering herd" of
retries, knocking it down again.

*Full jitter* solves this: instead of waiting `baseDelay * 2^attempt`,
wait a uniform random value in `[0, baseDelay * 2^attempt]`. The
expected wait is still exponential, but the variance spreads retries
across the recovery window.

This is what AWS recommends in its retry guidance and what most
production retry libraries (Resilience4j, Polly, tenacity) implement
by default. Do not skip the jitter.

### Idempotency — when retrying is dangerous

Retrying a GET is safe. Retrying a DELETE is usually safe (deleting
an already-deleted record is a no-op). Retrying a POST can create
duplicate records: if the original request succeeded but the response
was lost in the network, the retry will create a second record on the
server.

The fix is *idempotency keys*. The client generates a unique
identifier per logical operation and sends it as a header
(`Idempotency-Key: <uuid>`). The server records the key and rejects
duplicate requests. Many production APIs use idempotency keys for
non-idempotent operations that need to be safely retryable; payment
APIs (Stripe, Square) implement this pattern explicitly, and an
IETF draft proposes standardizing the header.

In Lab 2, you will retry GET operations only. Real production systems
that retry POSTs will use idempotency keys — but that is beyond the
lab's scope.

### Choosing `maxAttempts`

Three attempts is the typical default. Fewer (1-2) wastes the recovery
opportunity for transient blips. More (5+) delays propagating the
failure to the caller, which is bad if the caller has its own latency
budget.

The total wait time grows quickly with `maxAttempts` because of the
exponential backoff. The retry loop sleeps after every failed attempt
*except* the last one (the last failure is rethrown to the caller),
so the number of waits is one fewer than the number of attempts. At
`baseDelay = 1s` with full jitter:

| `maxAttempts` | Max possible total wait | Typical total wait |
|---------------|-------------------------|--------------------|
| 3             | 1 + 2 = 3 s             | ~1.5 s (50% of max) |
| 5             | 1 + 2 + 4 + 8 = 15 s    | ~7.5 s             |

Five attempts is rarely worth it; the caller usually wants to know
about a sustained failure sooner than ~15 seconds of waiting.

### Distinguishing transient from permanent

The retry helper above treats `IOException` as transient and
everything else as permanent. This is a conservative default. In a
real implementation, classify based on what you can observe:

- *Transient:* network errors, timeouts, HTTP 502/503/504, database
  deadlocks, rate-limit responses (HTTP 429 — and respect any
  `Retry-After` header).
- *Permanent:* HTTP 400/401/403/404, malformed input, authentication
  failures, validation errors. Retrying these wastes time and
  amplifies failure rate.

Misclassification in either direction is harmful. Retrying a 400
because you classified it as transient adds load to a downstream
service that is already telling you the request is bad. Treating a 503
as permanent gives up on a service that may recover in seconds.

---

## Detailed Notes — Circuit Breaker

### Example (Java)

A minimal thread-safe circuit breaker that wraps any `Callable<T>`.
Tracks consecutive failures, transitions through CLOSED → OPEN →
HALF_OPEN → CLOSED, and rejects calls while OPEN.

```java
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

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
        // If OPEN, check whether we can move to HALF_OPEN.
        if (state == State.OPEN) {
            if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) >= 0) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException();
            }
        }

        try {
            T result = operation.call();
            // Success: reset counters and close the breaker.
            consecutiveFailures = 0;
            state = State.CLOSED;
            return result;
        } catch (Exception e) {
            consecutiveFailures++;
            // Any failure in HALF_OPEN, or hitting the threshold in
            // CLOSED, transitions to OPEN.
            if (state == State.HALF_OPEN
                    || consecutiveFailures >= failureThreshold) {
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
```

```java
public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException() {
        super("circuit breaker is open");
    }
}
```

Used at the call site:

```java
CircuitBreaker breaker = new CircuitBreaker(5, Duration.ofSeconds(30));

Map<String, Object> profile = breaker.call(
        () -> service.fetchUserProfile(userId));
```

### Choosing `failureThreshold`

The threshold defines what counts as "sustained" failure. Too low (1-2)
and the breaker trips on noise — a single bad request takes the whole
downstream offline. Too high (50+) and the breaker is slow to react, by
which point the downstream is overloaded.

The right value depends on your call volume:

- High-volume services (hundreds of calls per second): 50% failure rate
  over a sliding window of 20+ calls. (This is what most production
  libraries default to.)
- Lower-volume services (a few calls per minute): 5 consecutive
  failures over any duration.

For Lab 2, a fixed count (e.g., 5 consecutive failures) is enough.
Sliding-window failure rates are the production-grade refinement.

### Choosing `openDuration`

How long should the breaker stay open before probing again? Long
enough that the downstream gets meaningful recovery time; short enough
that callers do not wait too long once it has recovered. 10-60 seconds
is the usual range.

A more sophisticated approach is exponential open-duration: the first
trip stays open for 10 seconds; if the probe fails, the next open
lasts 20 seconds; then 40; capped at some maximum. This protects a
downstream that is recovering slowly. Lab 2 uses a fixed open-duration
for simplicity.

### What to return when the breaker is open

The example above throws `CircuitBreakerOpenException`. This forces
callers to handle the broken-downstream case explicitly. Alternatives,
in order of complexity:

1. **Throw** (the default). Callers handle it like any other failure.
2. **Return a cached value.** If you have a recent successful result,
   serve it. This is "fallback to stale data" — common for read-heavy
   APIs.
3. **Return a default.** A sensible empty/safe value (`null`,
   `Collections.emptyList()`, a default config). Use only when the
   caller can genuinely tolerate the default.

Choose based on what the *caller* can do. If the caller has no
graceful degradation, throwing is the only honest option.

### Half-open probe behavior

When the open-duration elapses, the breaker transitions to HALF_OPEN.
The next call through the breaker is the "probe": one real call to the
downstream to find out whether it has recovered.

The probe is treated specially:
- If it succeeds, transition to CLOSED. The downstream is healthy.
- If it fails, transition back to OPEN. Reset the open-duration
  timer so the next probe is `openDuration` from now.

In high-concurrency systems, you do not want every concurrent caller
to be a probe simultaneously. The standard refinement is "single
probe": while HALF_OPEN, exactly one call is admitted to test the
downstream; concurrent callers are rejected as if the breaker were
still OPEN. The example above is intentionally simplified for lab
use: the `synchronized` method serializes calls through the breaker,
but it does not implement a production-grade single-probe policy
with explicit rejection of concurrent half-open callers.

---

## Composing Retry and Circuit Breaker

The two patterns address different problems. Retry handles *transient
single-call failures*; circuit breaker handles *sustained
multi-call failures*. They compose, but the order matters:

```
  caller → circuit breaker → retry → downstream
                  (outer)    (inner)
```

That is, the circuit breaker wraps the retry. Reasoning:

- An exhausted retry counts as **one logical failure** for the circuit
  breaker, not three (or however many attempts). If the inner retry
  exhausts after three attempts, the breaker increments its failure
  counter by one. Otherwise, every retry storm would trip the breaker
  on the first failed operation.
- `CircuitBreakerOpenException` should **not** trigger a retry. When
  the breaker is open, the downstream is known to be down — retrying
  immediately is exactly wrong. Add a check in your retry helper:

```java
if (e instanceof CircuitBreakerOpenException) {
    throw e;  // do not retry
}
```

The combined call site:

```java
CircuitBreaker breaker = new CircuitBreaker(5, Duration.ofSeconds(30));

Map<String, Object> profile = breaker.call(() ->
    Retry.withBackoff(
        () -> service.fetchUserProfile(userId),
        3,
        Duration.ofSeconds(1)));
```

This reads outer-to-inner: the breaker may reject the whole operation;
if it admits the call, the retry handles transient failures inside
its admission.

---

## Pitfalls

These are the mistakes students most often make on first
implementation. None are subtle once you know to look for them, but
they are easy to miss while focused on the happy path.

### Retry pitfalls

1. **No jitter.** Pure exponential `wait = base * 2^attempt` produces
   synchronized retry storms. Always add full jitter:
   `wait = random(0, base * 2^attempt)`.

2. **Retrying non-idempotent operations.** Retrying a POST without an
   idempotency key can create duplicate records. Restrict retries to
   GET / idempotent operations until you have a strategy for
   idempotency.

3. **Retrying permanent failures.** A 400 Bad Request will not become
   a 200 OK on retry. Classify failures before retrying; only retry
   the ones that can succeed on the next attempt.

4. **No upper bound on attempts.** A `while(true)` retry loop can hang
   the caller indefinitely. Always cap `maxAttempts`.

5. **Naked `Thread.sleep(...)` without `InterruptedException`
   handling.** If the calling thread is interrupted during a sleep,
   the retry should propagate the interrupt rather than swallow it.
   Either re-interrupt the thread or wrap in
   `try / catch InterruptedException` and rethrow.

### Circuit breaker pitfalls

1. **Counting attempts, not operations.** If retry is inside the
   breaker (the recommended order), the breaker should see one
   logical failure per exhausted retry, not three. Place the retry
   call *inside* the breaker's `call(...)`, not around it.

2. **Threshold counts not reset on success.** If a counter increments
   on failure but never resets on success, the breaker will trip on
   any 5 failures *across the lifetime of the process*. Reset
   `consecutiveFailures = 0` on every success.

3. **No half-open transition.** Without a half-open probe, a breaker
   that trips once will stay open forever — the downstream may have
   recovered, but the breaker will never test for it. Always include
   the open-duration timeout and the HALF_OPEN probe.

4. **Probe failure does not reset the timer.** When the half-open
   probe fails, transition back to OPEN *and* update `openedAt =
   now`. Otherwise the breaker oscillates rapidly between OPEN and
   HALF_OPEN.

5. **State mutated without synchronization.** Multiple threads
   reading and writing `state`, `consecutiveFailures`, and `openedAt`
   without synchronization can leave the breaker in an inconsistent
   state. Either guard each method with `synchronized` (the example
   above) or use `AtomicInteger` / `AtomicReference` for finer
   control.

### Composition pitfalls

1. **Retrying `CircuitBreakerOpenException`.** A breaker that has
   opened is signalling "do not call." Retrying is exactly wrong.
   Treat the breaker-open exception as permanent in the retry
   classifier.

2. **Breaker inside the retry.** This is the wrong order. If the
   breaker is inside the retry, every retry attempt is a separate
   admission decision, and the breaker sees N failures per logical
   operation instead of one. The retry should be inside; the breaker
   should be outside.

---

## Further Reading

The two patterns above are foundational; production libraries
implement them with more sophistication (sliding-window failure
counting, bulkheads, fallback chains, metrics). The standard
references are Michael Nygard's *Release It!* (2nd edition,
Pragmatic Bookshelf, 2018) for the conceptual foundations and the
*Resilience4j* documentation for a well-designed JVM implementation.
You do not need either for Lab 2 — the implementations in this
reference sheet are complete for the lab — but both are worth
returning to when you encounter these patterns in production.
