# Instructor Notes — Lab 1 Defect Inventory

> ⚠ **Do not share this file with students.** This is the answer key
> for the fault-analysis worksheet, plus operational notes for demoing
> each defect in lecture or office hours.

This document maps each of the four named reliability defects to its
location in `UserProfileService.java`, explains what makes it a defect,
provides a model worksheet entry, and describes how to demonstrate the
defect's consequences if a student asks "but the code worked when I
ran it — what's the problem?"

---

## Defect A — Unhandled `SocketTimeoutException`

**Location.** `fetchUserProfile`, lines 50–65, specifically the
`url.openConnection()` and `conn.getInputStream()` calls (lines 52, 57).

**What's wrong.** The `HttpURLConnection` is created with no
`setConnectTimeout()` or `setReadTimeout()` configuration. If the
remote service is slow or hung, the call blocks indefinitely. The
method declares `throws IOException` broadly, which technically covers
`SocketTimeoutException`, but no timeout is *configured* in the first
place — so the method may never throw a timeout at all. A caller that
expects "fail fast on a slow server" gets "hang forever" instead.

**Model worksheet entry:**
- *Failure scenario:* Remote service slow or unreachable; TCP connection
  established but no response data ever arrives.
- *Current handling:* None — no timeouts configured.
- *Impact:* The calling thread hangs indefinitely. In a web server, this
  exhausts the request thread pool under load.
- *Proposed fix:* Set `conn.setConnectTimeout(5000)` and
  `conn.setReadTimeout(10000)` before the call. (Lab 2 will go further:
  use modern `HttpClient.newBuilder().connectTimeout(...)`.)
- *Why the AI missed it:* The prompt asked for "fetches user profile
  data." It said nothing about latency, hang scenarios, or
  service-level objectives. The model produced code that satisfies the
  literal request and stops.

**Demo.** To make the defect visible: edit `Main.java` so the canned
handler calls `Thread.sleep(60_000)` before writing the response. The
demo will hang for a full minute (or whatever sleep value you pick)
with no error.

---

## Defect B — Resource leak from missing `finally`

**Location.** `updateProfile`, lines 76–92, specifically the
`getOutputStream()` / `out.close()` / `conn.disconnect()` sequence.

**What's wrong.** The `OutputStream` is obtained on line 84 and closed
on line 87, but with no `try/finally` or try-with-resources protecting
the close. If `out.write()` (line 85) or `json.writeValueAsString()`
(line 83) throws, `out.close()` and `conn.disconnect()` never run. The
connection leaks. Similar concern for `conn.disconnect()` on line 90 —
if `conn.getResponseCode()` throws, the connection is also never
disconnected.

**Model worksheet entry:**
- *Failure scenario:* JSON serialization fails (e.g., a `Map` value
  contains a non-serializable object), or network write fails midway
  through the body.
- *Current handling:* None — no `finally` or try-with-resources.
- *Impact:* Connection and socket resources leak. Under load, the host
  exhausts available file descriptors or hits OS connection limits.
- *Proposed fix:* Wrap `getOutputStream()` in try-with-resources, and
  wrap the entire connection in a `try/finally` that calls
  `conn.disconnect()` in the `finally` block.
- *Why the AI missed it:* The happy-path narrative is "open, write,
  close, disconnect." The model wrote the steps in sequence as a
  beginner might. Cleanup-on-exception is a separate concern that
  requires modeling failure as part of the design, not as a follow-up.

**Demo.** Edit the canned handler in `Main.java` to close the request
mid-stream before reading the body. The leak is harder to surface in a
short demo than the timeout defect, but you can show that the method
throws and the connection object is never disconnected.

---

## Defect C — Generic `catch(Exception)` that logs without recovering

**Location.** `getCachedProfile`, lines 98–107, specifically lines
103–106 (the `catch (Exception e)` block).

**What's wrong.** The `catch (Exception e)` block swallows everything:
- `NoSuchFileException` (expected — no cache entry yet, this is fine)
- `IOException` from disk corruption (rare, should alert)
- `JsonParseException` from malformed JSON (could mean a partial write)
- `SecurityException` from filesystem permissions (deployment issue)
- and `NullPointerException` or `OutOfMemoryError` if `parseProfile`
  has an internal bug

All five are treated identically: log a one-line message and return
`null`. The caller cannot distinguish "no cache entry" from "cache
corrupted" from "filesystem broken" from "code bug." Worse, the JavaDoc
says `null` means "no cache entry" — so the broader interface lies.

**Model worksheet entry:**
- *Failure scenario:* Any non-trivial failure reading or parsing the
  cache file.
- *Current handling:* Catches `Exception`, prints to stderr, returns
  `null`.
- *Impact:* Distinct failure modes are conflated; downstream code
  cannot make informed recovery decisions; bugs are silently masked.
- *Proposed fix:* Catch specific exceptions individually.
  `NoSuchFileException` → return `null` silently (expected case).
  Other `IOException` or `JsonParseException` → log at warning level
  and return `null` (cache is broken but operation can continue).
  `RuntimeException` → propagate (do not swallow programming errors).
- *Why the AI missed it:* `catch (Exception e)` is the smallest amount
  of code that compiles and "handles" exceptions. Differentiated
  recovery requires the model to reason about which exceptions mean
  what — a step that requires the prompt to invite it.

**Demo.** Corrupt the cache file mid-run: after the demo creates the
`u-101.json` cache entry, manually edit it to invalid JSON, then call
`getCachedProfile` again. The method returns `null` with no indication
that the cache is broken (rather than just unpopulated).

---

## Defect D — No retry for transient failures

**Location.** `loadProfileBatch`, lines 116–133, specifically lines
120–127 (the for-each loop).

**What's wrong.** Each profile is fetched exactly once. If
`fetchUserProfile` throws `IOException` (network blip, temporary 503
from the server, DNS hiccup), the user is added to `failed` and the
loop moves on. Transient failures — exactly the kind retries are made
for — produce permanent data loss for that user in the batch.

**Model worksheet entry:**
- *Failure scenario:* Transient network error or transient 5xx response
  during a batch read.
- *Current handling:* Single attempt; user added to `failed` list on
  any `IOException`.
- *Impact:* Batches that include any flaky users return incomplete
  data. In production, a flaky third-party API turns into systematically
  missing data rather than recoverable noise.
- *Proposed fix:* Retry transient failures with exponential backoff (up
  to ~3 attempts; wait 1s, 2s, 4s between attempts). Distinguish
  transient (5xx, network) from permanent (4xx, malformed) — only retry
  the transient cases. This is exactly what Lab 2 builds.
- *Why the AI missed it:* The prompt said "fetch profiles for multiple
  users." A for-each loop satisfies the literal request. Retry-with-
  backoff is a separate concern that requires reasoning about how
  failures distribute across calls, not how a single call succeeds.

**Demo.** Modify `Main.java`'s canned handler to fail the first call
for `u-102` with HTTP 503 but succeed on the second. The batch call
will report `u-102` as failed even though a retry would have
succeeded.

---

## How the defects sequence into Lab 2

Each defect maps to a fault-tolerance pattern Lab 2 teaches:

| Defect | Lab 2 pattern |
|--------|---------------|
| A (no timeout)   | Modern `HttpClient` with `connectTimeout(...)` and per-request `timeout(...)` |
| B (resource leak) | Try-with-resources / proper `finally` discipline |
| C (generic catch) | Specific exception handling with distinct recovery |
| D (no retry)     | Retry with exponential backoff; circuit breaker for sustained failure |

Lab 2 has students fix B and C as warm-up, then implement A's timeout
plus D's retry-with-backoff plus a circuit breaker over the same
codebase. By the end of Lab 2, the service is genuinely
fault-tolerant.

## Audit-runner output on this codebase

For your reference (and to demonstrate the analyzer in Lab 3): running
the audit runner from Section 3.2 of the paper against this codebase
should report try-block presence (the `try (BufferedReader...)` and
`catch (Exception e)` blocks) but flag the generic catch and the
absence of retry/timeout patterns. The codebase is intentionally
similar in shape to the AI-generated samples in the audit corpus, so
the analyzer reports it as recognizably AI-generated for the purposes
of Lab 3's "Analyzer in the Loop" exercise.

## Pacing suggestions

The fault analysis worksheet for Lab 1 is designed for ~50 minutes of
in-pair work. Suggested allocation:

- Read the README and skim `UserProfileService.java` (10 min)
- Run the demo, confirm happy path (5 min)
- Work through the four methods, completing the worksheet (30 min)
- Pair discussion + share-out (10 min)

Students who finish early can attempt to *describe* fixes (without
implementing them — that is Lab 2). Discourage premature coding;
the critique-only constraint is part of the Lab 1 scaffolding.
