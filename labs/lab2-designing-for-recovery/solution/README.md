# Lab 2 — Instructor Reference Solution

**Do not commit this directory to the public repository.** Keep it
outside the artifact tree (suggested location:
`~/Developer/lab2-instructor-solution/`). These files exist solely to
validate that the failure-injection harness behaves correctly when
the starter classes are implemented as expected.

## What's in this directory

- `Retry.java` — reference implementation of
  `Retry.withBackoff(Callable, int, Duration)`.
- `CircuitBreaker.java` — reference implementation of
  `CircuitBreaker.call(Callable)` and `currentState()`.

Both files use the same package declaration
(`edu.example.userprofile.resilience`) as the starter files in the
Lab 2 project, so they drop in as direct replacements.

## How to validate the harness

Open a terminal at the root of the Lab 2 project:

```bash
cd ~/Developer/"AI RELIABILITY"/labs/lab2-designing-for-recovery
```

1. **Save the starter files to a backup location** so you can restore
   them after validation:

   ```bash
   cp src/main/java/edu/example/userprofile/resilience/Retry.java \
      ~/Developer/lab2-starter-backup-Retry.java
   cp src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java \
      ~/Developer/lab2-starter-backup-CircuitBreaker.java
   ```

2. **Swap in the reference solution:**

   ```bash
   cp ~/Developer/lab2-instructor-solution/Retry.java \
      src/main/java/edu/example/userprofile/resilience/Retry.java
   cp ~/Developer/lab2-instructor-solution/CircuitBreaker.java \
      src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java
   ```

3. **Run the required test suite:**

   ```bash
   mvn test
   ```

   **Expected outcome:** all 17 required tests pass. The Maven output
   should show `Tests run: 17, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

4. **Optionally, run the extension suite:**

   ```bash
   mvn test -P optional
   ```

   **Expected outcome:** all 3 optional tests pass (jitter,
   concurrency, extended interrupt). The jitter test is statistical
   and could in principle false-fail on unusually consistent
   hardware, but in practice always passes.

5. **Restore the starter files** so the public artifact is back to
   its blank-slate state:

   ```bash
   cp ~/Developer/lab2-starter-backup-Retry.java \
      src/main/java/edu/example/userprofile/resilience/Retry.java
   cp ~/Developer/lab2-starter-backup-CircuitBreaker.java \
      src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java
   ```

   Confirm with `mvn test` that you are back to 17 failures.

## What validation proves

A successful run validates two things:

- **The harness has no bugs.** A student who implements the patterns
  correctly per `pattern_reference.md` will see all 17 tests pass.
  No spurious test failures from harness-side mistakes.
- **The reference solution itself is correct.** Future-you (a year
  from now, when teaching the lab) can read the reference and trust
  that it matches the harness.

If any of the 17 tests fail when running the reference solution, the
mismatch is between the test expectations and the reference. Open
the failing test, read its assertion, and determine which side is
correct — the test or the reference. The fix usually goes in the
test, since the reference is constructed to match the public
behavior students should learn.

## Notes on the implementation choices

A few small choices were made to keep the reference clean and
pedagogically honest:

- `Retry.isTransient(Exception e)` treats only `IOException` as
  transient (and explicitly rejects `CircuitBreakerOpenException`).
  Production code may classify more carefully — HTTP 5xx vs 4xx,
  SQL transient vs constraint, etc. — but for Lab 2 the default
  classifier is sufficient and what the test harness expects.

- The retry loop uses *full jitter*: `random(0, ceiling)` rather
  than the slightly more common "equal jitter" formula
  (`ceiling/2 + random(0, ceiling/2)`). Full jitter is what AWS
  recommends and what most production libraries default to. Both
  are correct; full jitter is simpler.

- The `CircuitBreaker.call(...)` method is `synchronized` for thread
  safety. This is the simplest correct approach. A production
  implementation might use `AtomicInteger` and `AtomicReference`
  for finer-grained concurrency, but at lab scale `synchronized`
  is appropriate.

- The breaker uses `consecutiveFailures` (a simple counter) rather
  than a sliding-window failure rate. Sliding-window failure
  detection is the production refinement (Resilience4j defaults to
  it). The pattern reference sheet describes both; the harness
  tests only the simpler consecutive-failure version.

These choices are documented in `pattern_reference.md`. The
reference solution implements the simpler-but-correct version that
the lab teaches.
