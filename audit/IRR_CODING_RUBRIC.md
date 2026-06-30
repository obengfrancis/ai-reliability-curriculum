# Coding Rubric — Reliability Indicators in AI-Generated Java

**Purpose.** You are the second independent coder for an inter-rater
reliability (IRR) study. You will examine 25 Java samples and code each
on ten reliability indicators using only the definitions below.

**Critical instructions for independence:**
- Code from the Java source files **only**. Do not consult the audit
  script's output, the first coder's codes, or any annotations.
- If you have questions about a definition, ask **before** you begin.
  Do not ask mid-coding — that can bias your codes toward an expected
  answer.
- Code all 25 samples on all ten indicators. Do not skip cells.
- Record your codes on the provided blank coding sheet
  (`coder2_blank.csv`), one row per sample, in the same sample order.
- When a definition is genuinely ambiguous for a sample, code your
  best judgment and note the ambiguity in a `notes` column; do not
  leave the cell blank.

You are coding what the code **actually contains**, not what it
*should* contain. A sample that omits all error handling is coded as
zeros across the handler indicators — that is a valid, common result,
not a coding error.

---

## The ten indicators

For each indicator, the table gives the column name, the type of value
to record, and the precise rule.

### 1. `has_try_block` — (0 or 1)

Does the sample contain **at least one** `try` block anywhere in its
source (including `try`, `try-catch`, `try-finally`, `try-with-resources`)?

- **1** if any `try` keyword introducing a block is present.
- **0** if the sample contains no `try` block at all.

*Note:* a method that declares `throws IOException` and propagates
without catching has **no** try block → code **0**. (Example:
`fileio_chatgpt_02` — "Throws IOException; explicit file validation"
→ `has_try_block = 0`.)

### 2. `try_blocks` — (non-negative integer)

How many distinct `try` blocks are in the sample? Count each `try`
keyword that introduces a block.

- A single `try { ... } catch { ... }` counts as **1**.
- Two separate try statements (e.g., one in `main`, one in a helper)
  count as **2**.
- Nested try blocks each count (an inner `try` inside an outer `try`
  body is **+1**).

### 3. `empty_catches` — (non-negative integer)

How many `catch` blocks have a body that is **empty or comment-only**?

- A `catch (X e) { }` with no statements → counts.
- A `catch (X e) { // ignore }` with only a comment → counts.
- A `catch` block containing any executable statement (even just
  `e.printStackTrace();` or a log call) → does **not** count.

Record the **count** of empty catch blocks (often 0).

### 4. `generic_catches` — (non-negative integer)

How many `catch` blocks catch a **generic** exception type rather than
a specific one? Generic types are:

- `Exception`
- `Throwable`
- `RuntimeException`

A `catch (IOException e)`, `catch (SQLException e)`, or
`catch (InterruptedException e)` is **specific** → does not count.
A `catch (Exception e)` → counts.

Record the **count** of generic catch blocks.

*Note:* a common source is a smoke-test `main` method that wraps a
call in `catch (Exception e)`. (Example: `api_copilot_01` — generic
catch in `main` → `generic_catches = 1`.)

### 5. `has_retry` — (0 or 1)

Does the sample contain **any retry construct** — code that
re-executes an operation after a failure?

Count as a retry (**1**) if you see **either**:
- A named retry API or idiom: Resilience4j `Retry`, Spring
  `@Retryable`, `RetryTemplate`, etc.
- A **hand-rolled retry loop**: a `for` or `while` loop whose body
  contains the operation **and** a `try/catch`, where a failure leads
  to another loop iteration (re-attempting the same operation), with
  a bounded attempt count.

Do **not** count as a retry (**0**):
- A loop that iterates over data/collections (e.g.,
  `for (String s : list)`), even if it contains a try/catch — that is
  iteration, not re-attempting a failed operation.
- A single try/catch with no loop around the operation.

*Decision test:* "If the operation fails, does this loop run it
again?" If yes → retry. If the loop would move on to the next data
item regardless → not a retry.

### 6. `has_timeout` — (0 or 1)

Does the sample configure or impose **any timeout** on a potentially
blocking operation?

Count as a timeout (**1**) if you see **any** of:
- Setter-based connection timeouts: `setConnectTimeout(...)`,
  `setReadTimeout(...)` on a connection object.
- Builder-based timeouts: `HttpClient.newBuilder().connectTimeout(...)`,
  `HttpRequest.newBuilder().timeout(...)`, `Duration`-based timeouts on
  a client/request.
- Call-based timeouts: `Future.get(timeout, TimeUnit)`,
  `ExecutorService.awaitTermination(timeout, TimeUnit)`,
  `lock.tryLock(timeout, TimeUnit)`, `queue.poll(timeout, TimeUnit)`.

Do **not** count (**0**):
- `Future.get()` with **no** timeout argument.
- `Thread.sleep(...)` (that is a delay, not a timeout on an operation).

*Note:* timeouts often appear **without** a surrounding try block
(setter-based config on an HTTP connection). Code `has_timeout = 1`
regardless of whether a try block is present. (Example:
`config_claude_01` — `has_try_block = 0` but `has_timeout = 1`,
"Uses HttpRequest (30s) timeouts.")

### 7. `has_circuit_breaker` — (0 or 1)

Does the sample contain a **circuit-breaker** construct?

Count as **1** if you see:
- A named circuit-breaker API: Resilience4j `CircuitBreaker`,
  `@CircuitBreaker`, Hystrix, etc.
- A hand-rolled state machine with CLOSED/OPEN/HALF-OPEN states (or
  equivalent) that trips after repeated failures and short-circuits
  subsequent calls.

Code **0** otherwise. (In practice this is expected to be 0 across all
samples.)

### 8. `has_backoff` — (0 or 1)

Does the sample contain an **explicit backoff schedule** — an
increasing or fixed delay between retry attempts?

Count as **1** if you see a delay (`Thread.sleep`, `Duration`-based
wait, scheduled delay) that is **between attempts of a retried
operation**, especially if the delay grows (exponential/linear
backoff).

Code **0** if there is no retry (no attempts to space out), or if a
delay exists but is not between retry attempts. (Expected to be 0
across all samples.)

### 9. `recovery_present` — (0 or 1)

Does the sample contain **any** fault-tolerance recovery mechanism —
i.e., at least one of {timeout, retry, circuit breaker, backoff}?

- **1** if any of indicators 5–8 is 1, **or** `has_timeout` (6) is 1.
- **0** if all of timeout, retry, circuit breaker, and backoff are 0.

*This is a derived "any mechanism present" flag.* In practice, because
retry/CB/backoff are expected to be 0 throughout, `recovery_present`
will equal `has_timeout` for nearly all samples — but code it
independently by checking all four mechanisms, rather than copying the
timeout value.

### 10. `propagation_violations` — (non-negative integer)

How many times does the sample catch an exception and **rethrow or
wrap it in a way that loses the original cause** (no exception
chaining)?

Count a violation when a `catch` block creates and throws a **new**
exception **without** passing the caught exception as the cause:

- `catch (SQLException e) { throw new RuntimeException("failed"); }`
  → **violation** (original `e` discarded; no cause chained).
- `catch (SQLException e) { throw new RuntimeException("failed", e); }`
  → **not** a violation (cause `e` is chained).
- `catch (SQLException e) { throw e; }` → **not** a violation
  (rethrowing the same exception).
- Simply declaring `throws SQLException` and not catching → **not** a
  violation (nothing is caught, so nothing is mis-propagated).

Record the **count** of propagation violations (often 0).

*Note:* `dbquery_chatgpt_02` catches `SQLException` and throws
`RuntimeException(e)` — because `e` **is** passed as the cause, this is
**not** a violation → `propagation_violations = 0`.

---

## Coding sheet format

Fill in `coder2_blank.csv`. It has one row per sample (25 rows), in the
fixed sample order, with these columns:

```
sample_id, has_try_block, try_blocks, empty_catches, generic_catches,
has_retry, has_timeout, has_circuit_breaker, has_backoff,
recovery_present, propagation_violations, notes
```

- Binary indicators (`has_*`, `recovery_present`): enter `0` or `1`.
- Count indicators (`try_blocks`, `empty_catches`, `generic_catches`,
  `propagation_violations`): enter a non-negative integer.
- `notes`: optional free text; use it to flag any sample where a
  definition was hard to apply.

Do not reorder rows. Do not leave cells blank.

---

## After coding

Return the completed `coder2_blank.csv`. The first coder will then run
the agreement computation (`compute_irr.py`), which reports, per
indicator: percent agreement, Gwet's AC1, and — for indicators with
variance — Cohen/Conger kappa. Disagreements will be examined
individually and resolved by discussion; resolved codes are then
compared against the audit runner's output.

You will **not** be named in the paper (the submission is
double-anonymized), but your independent coding is what makes the
validation a genuine inter-rater study rather than a single-coder
consistency check. Thank you for coding carefully and independently.
