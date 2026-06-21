# Lab 2 — Designing for Recovery

A failure-injection test harness that verifies your implementation
of two fault-tolerance patterns: **retry with exponential backoff**
and **circuit breaker**. Open `pattern_reference.md` for the
patterns themselves; read this file for how the harness is
organized and how to run it.

## What you will implement

Two starter classes have `TODO` markers and intentionally throw
`UnsupportedOperationException` until you implement them:

- `src/main/java/edu/example/userprofile/resilience/Retry.java` —
  the `withBackoff(...)` method.
- `src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java` —
  the `call(...)` method and the `currentState()` reader.

A third class is already complete and you do not need to modify it:

- `CircuitBreakerOpenException.java` — thrown by the breaker when
  it is OPEN.

The class names and method signatures match the contracts in
`pattern_reference.md` exactly. The test harness imports these
classes directly, so changing the signatures will break the tests
at compile time.

## How to run the tests

The required suite (what your implementation must pass):

```bash
mvn test
```

The optional/extension suite (jitter, concurrency, longer scenarios):

```bash
mvn test -P optional
```

The required suite is about 17 tests, completes in under five
seconds, and verifies the core behaviour of both patterns plus
their composition. The optional suite contains statistical and
concurrent tests that may occasionally produce false negatives
on slow hardware — they are valuable for advanced students or
self-checking, but the lab is graded on the required suite only.

## Reading test failures

The test failure messages are written to be instructional, not
just diagnostic. When a test fails, read the assertion message
carefully — it usually names the specific implementation choice
you got wrong and points back to the relevant section of
`pattern_reference.md`.

For example, a failing message might read:

> *Expected exactly 1 attempt for a permanent (non-IOException)
> failure; your helper made 5 attempts. Are you treating all
> exceptions as transient? Only IOException should retry by
> default — see Retry.isTransient().*

That message tells you (a) what behaviour the test expected,
(b) what your code produced, (c) the likely cause, and (d) where
in the reference material to look. If you find yourself reading
the test source to figure out what failed, the message is not
doing its job and you should let the instructor know.

## Test organization

```
src/test/java/edu/example/userprofile/
├── resilience/
│   ├── RetryTest.java           ← 6 required retry tests
│   ├── CircuitBreakerTest.java  ← 7 required circuit-breaker tests
│   └── IntegrationTest.java     ← 4 required integration tests
├── support/
│   └── ControllableFakeServer.java   ← failure-injection HTTP server
└── optional/
    └── ExtensionTests.java      ← jitter, concurrency (run with -P optional)
```

The integration tests apply your two patterns to the
`UserProfileService` from Lab 1, demonstrating how the patterns
compose with real code. You do not need to modify
`UserProfileService` itself; the integration tests wrap calls to
it with your `Retry.withBackoff(...)` and `CircuitBreaker.call(...)`
at the call site. A deeper refactor (moving the patterns inside
the service) is left for Lab 3.

## The failure-injection server

The harness uses a small in-process HTTP server with a fluent
API for injecting failures. You will see usages like:

```java
server.failNext(2, 503).thenSucceed();
```

which scripts the next two requests to return HTTP 503, followed
by successful responses for everything after. You do not interact
with the server directly — it is set up by the test harness and
used to give your patterns realistic operations to wrap.

If you want to understand what a particular failure scenario
looks like at the HTTP level, open
`src/test/java/edu/example/userprofile/support/ControllableFakeServer.java`.

## Suggested order of work

1. Read `pattern_reference.md` end-to-end (about 15 minutes).
2. Implement `Retry.withBackoff(...)`. Run `mvn test` — the retry
   tests should pass before you touch the circuit breaker.
3. Implement `CircuitBreaker.call(...)` and `currentState()`.
   Run `mvn test` again — the circuit-breaker tests should now
   pass too.
4. Verify the integration tests pass. These exercise both
   patterns together; they pass automatically once both are
   correct individually.
5. Optionally, run `mvn test -P optional` and inspect any
   failures in the extension suite.

## What this lab assesses

The harness checks correctness of the patterns themselves, not
how you applied them. The Lab 2 rubric also evaluates:

- A short reflection on the trade-offs you made (e.g., your
  choice of jitter strategy, your failure threshold, what your
  breaker returns when OPEN).
- Code clarity of your implementations.
- Whether your `isTransient` classification matches the
  guidance in `pattern_reference.md`.

See your instructor's syllabus for the rubric specifics.
