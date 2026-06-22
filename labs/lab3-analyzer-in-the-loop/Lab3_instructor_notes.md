# Instructor Notes — Lab 3 Issue Inventory

> ⚠ **Do not share this file with students.** This is the answer key
> for Lab 3, plus grading guidance and observations on what
> "designer-level reasoning" looks like at this stage.

This document catalogs the 8 reliability concerns deliberately
planted in the Lab 3 codebase, the analyzer signal (or absence
thereof) that each one produces, what reasoning a strong student
should articulate, and what a defensible fix looks like.

## At-a-glance inventory

| # | File / method | Analyzer signal | Severity | Visibility |
|---|---|---|---|---|
| 1 | `EmailGateway.send()` | `empty_catches=1` | Medium | Direct |
| 2 | `ProfileService.loadOrFetch()` | `generic_catches=1` | High | Direct |
| 3 | `EmailGateway.send()` | `has_timeout=false` (file shows `recov: no`) | Critical | Indirect (absence) |
| 4 | `AuditLogger.append()` | `has_timeout=true` but `has_retry=false` | High | Indirect (partial — present-but-incomplete) |
| 5 | `ProfileRepository.findById()` | `propagation_violations=2` | Medium | Direct |
| 6 | `CacheStore.write()` | none — no try-block, no catch | High | Invisible |
| 7 | `BatchApi.fetchAll()` | none — looks fine on counts | Critical | Invisible |
| 8 | `CacheManager.get()` | none — looks like graceful degradation | High | Invisible |

The defensible "three most critical" is **{#3, #7, #8}** in roughly
that order. A student who picks any subset that includes #7 and
either #3 or #8 is on solid ground. The most concerning answer
would be a student who picks the three direct analyzer findings
({#1, #2, #5}) without identifying any of the invisible issues —
that means they treated the analyzer as a verdict, not a starting
point.

## Detailed inventory

### Issue 1 — `EmailGateway.send()`: empty catch

**Location.** `external/EmailGateway.java`, lines 42-44.

**What's wrong.** `catch (IOException e) { }` swallows the
exception silently. The method returns `false` but the caller has
no way to distinguish "the gateway returned a non-2xx response"
from "the call failed entirely with an I/O error" from
"everything's fine, the email simply didn't go out."

**Analyzer signal.** `empty_catches=1` — directly flagged in the
per-file table.

**Severity.** Medium. The email is a side-effect of profile reads,
not a critical-path operation. But the silent swallow makes
debugging harder if email delivery ever degrades.

**Expected student reasoning.** A strong student notes that the
empty catch hides at least three distinct failure modes (DNS,
TCP, malformed response) and that the method's return value
(`false`) confounds protocol-level rejection with infrastructure
failure. Weaker reasoning would just say "empty catch is bad" and
move on.

**Possible fix.** Log the exception at warning level with the
recipient and a stack-trace summary. Optionally, differentiate
the return type to convey "send failed permanently" vs "send may
have succeeded but the response was lost."

### Issue 2 — `ProfileService.loadOrFetch()`: generic catch

**Location.** `core/ProfileService.java`, lines 35-39.

**What's wrong.** `catch (Exception e)` swallows multiple distinct
failure modes:
- `ProfileNotFoundException` from the repository (legitimate "no
  such user").
- `IOException` from `cache.put` (disk write failed — should not
  silently mask).
- Any `RuntimeException` from a programming bug.

All three produce the same `null` return. The caller can't
distinguish "no profile exists" from "disk is broken" from "code
has a bug."

**Analyzer signal.** `generic_catches=1` — directly flagged.

**Severity.** High. This is the public-API surface — the rest of
the system reads from this method.

**Expected student reasoning.** The strongest version of the
answer explicitly names two or three of the distinct failure
modes and articulates why conflating them is harmful. A weaker
answer notes only that `catch (Exception)` is bad style.

**Possible fix.** Replace with specific catches:
- `ProfileNotFoundException` → return null silently (expected case)
- `IOException` (from cache.put) → log warning, return the
  fetched profile anyway (cache failed, but the data is loaded)
- Let `RuntimeException` propagate

### Issue 3 — `EmailGateway.send()`: no timeout configured

**Location.** `external/EmailGateway.java`, lines 26-37.

**What's wrong.** The `HttpURLConnection` is created without
`setConnectTimeout` or `setReadTimeout`. If the email gateway
becomes slow or hangs, the calling thread blocks indefinitely.

**Analyzer signal.** Absence — the file's `recov` column shows
`no` (no recovery pattern detected). This is *indirect*: the
student has to notice that EmailGateway makes an external HTTP
call and that the analyzer reports no timeout pattern.

**Severity.** Critical. The email gateway is called synchronously
inside the profile-load flow via `NotificationDispatcher`. A slow
email gateway can therefore hang user-facing profile reads. In a
web server context, a thread pool starvation under load.

**Expected student reasoning.** Strong students notice that
EmailGateway is called from `NotificationDispatcher`, which is
called from `ProfileService.loadOrFetch`, which is the public API
— so a hung email gateway hangs the API. The "blast radius"
reasoning is what differentiates designer-level analysis from
checklist-level analysis.

**Possible fix.** Add `conn.setConnectTimeout(5000)` and
`conn.setReadTimeout(10000)`. Better: move email dispatch to an
async/queued workflow so it never blocks the profile-read path.

### Issue 4 — `AuditLogger.append()`: no retry on transient failure

**Location.** `external/AuditLogger.java`.

**What's wrong.** The audit logger configures a timeout (good) but
makes a single attempt. A transient network blip during a
compliance-relevant audit event causes that event to be silently
lost. The audit log is supposed to be the source of truth for
"what happened" — losing entries undermines its purpose.

**Analyzer signal.** Partial — `has_timeout=true` and
`has_retry=false`. The overall `recovery_present=true` reads as
"this file has recovery" if you skim the totals. A student who
inspects the per-pattern counts will notice the missing retry.

**Severity.** High. Audit logs are compliance artifacts. Silent
data loss in an audit log is a serious operational concern,
distinct from "an email might not go out."

**Expected student reasoning.** This is the strongest test of
whether a student actually read the analyzer report carefully.
The headline "files with recovery pattern: 1" looks fine. The
detail "files with retry pattern: 0" tells the real story. A
student who articulates the distinction is operating at the
right level for the designer role.

**Possible fix.** Add retry-with-backoff (using a Resilience4j
helper or the Lab 2 implementation). 3 attempts at 100ms / 200ms
backoff. Consider whether failed audit events should be queued
for later retry rather than dropped on permanent failure.

### Issue 5 — `ProfileRepository.findById()`: propagation issue

**Location.** `storage/ProfileRepository.java`, two places — the
catch in the connection-setup section and the catch in the
read section.

**What's wrong.** Both catches do
`throw new ProfileNotFoundException("...")` without passing the
caught `IOException` as the cause. The original exception's
stack trace and message are lost. Downstream debugging is
significantly harder.

**Analyzer signal.** `propagation_violations=2` — directly
flagged (and the count of 2 is a useful pedagogical signal: both
occurrences are detected).

**Severity.** Medium. Doesn't change correctness — the right
information about "no profile exists" still reaches the caller.
But it makes incident response harder when something does go
wrong.

**Expected student reasoning.** Students should articulate that
wrapping an exception without `getCause()` loses the diagnostic
chain — *but also* that the larger problem here is conflating
"can't reach the store" (operational failure) with "no profile
exists" (legitimate not-found). The right fix isn't just adding
the cause; it's differentiating the two cases.

**Possible fix.** Pass `e` as the cause:
`throw new ProfileNotFoundException("...", e);` Better:
introduce a distinct `ProfileStoreUnavailableException` for the
operational-failure case, and reserve `ProfileNotFoundException`
for the "store returned 404" case.

### Issue 6 — `CacheStore.write()`: resource leak

**Location.** `storage/CacheStore.java`, lines 25-30.

**What's wrong.** The `BufferedWriter` is created on line 26 but
never closed via try-with-resources or `finally`. If `writer.write`
or `writer.flush` throws, the file handle leaks. Under sustained
write failures, the process can exhaust its file descriptor limit.

**Analyzer signal.** None. The analyzer counts try blocks; a
method with no try block has nothing to flag. The absence of
resource management is invisible to a regex-based check.

**Severity.** High. Resource exhaustion is one of the classic
production failures.

**Expected student reasoning.** A student who finds this is
specifically looking at "what happens to the writer if write
throws" — they're reading the code with failure modes in mind,
which is the designer mindset Lab 3 is trying to develop.

**Possible fix.** Use try-with-resources:
`try (BufferedWriter writer = new BufferedWriter(...)) { ... }`.
Same applies to `FileWriter` underneath.

### Issue 7 — `BatchApi.fetchAll()`: no per-call timeout

**Location.** `api/BatchApi.java`, around line 39.

**What's wrong.** Each future's `.get()` has no timeout. If ANY
single profile load hangs (because, say, `ProfileRepository.findById`
hangs on a network call to an unreachable store), then `.get()`
blocks forever for that future. The for-loop is sequential over
the futures map, so the batch hangs on the first hung future and
never even checks the others.

**Analyzer signal.** None. The code uses `ExecutorService` and
`Future`, the catches are specific, no try blocks are missing.
The analyzer has no way to see "Future.get() with no TimeUnit is
a hang risk in a batch context."

**Severity.** Critical. The batch API is a user-facing endpoint
that's supposed to load multiple profiles efficiently. One hung
backend turns it into "load nothing, slowly." This is a
denial-of-service failure mode.

**Expected student reasoning.** This is the strongest test of
whether a student is reading the code for *failure modes*. The
code looks perfectly fine on a happy-path read. A student who
spots this is asking "what does this code do when a downstream
call hangs?" — which is the question Lab 3 wants them to be
asking.

**Possible fix.** Bound each `.get()` with a timeout:
`future.get(5, TimeUnit.SECONDS)`. Handle the resulting
`TimeoutException` by cancelling the future and recording the
user as failed in the batch. Optionally, set up the executor with
a per-task timeout via a `ScheduledExecutorService` wrapper.

### Issue 8 — `CacheManager.get()`: silent stale fallback

**Location.** `core/CacheManager.java`, lines 30-43.

**What's wrong.** On `IOException` from the disk store (which
includes JSON parse failures because Jackson wraps them as
`IOException`), the method silently falls back to `memory.get(userId)`.
The caller has no way to know that the disk read failed. If the
disk cache file is corrupted (partial write, encoding issue), the
caller may get stale data from minutes or hours ago and present
it as current.

**Analyzer signal.** None. The catch is specific (`IOException`),
not empty, not generic, and contains a return statement. The
analyzer sees nothing wrong.

**Severity.** High. Silent data corruption is one of the worst
failure modes — it produces wrong answers that downstream systems
treat as right. The fact that it looks like graceful degradation
on first read is exactly why it's dangerous.

**Expected student reasoning.** A strong student articulates the
distinction between "graceful degradation with caller awareness"
and "silent fallback that hides the failure." The fix is not to
fail loudly — it's to give the caller a way to know.

**Possible fix.** Either propagate the IOException, or return a
typed wrapper that distinguishes "fresh", "stale", and "no data".
Logging the exception is the minimum acceptable fix; changing the
return contract is the strong fix.

## How to grade the worksheet

A satisfactory submission identifies at least one of the three
"invisible" issues (#6, #7, #8) in their top three. A strong
submission identifies at least two of the three invisible issues
AND articulates the analyzer-as-starting-point reasoning
explicitly in the reflection.

Weight the sections roughly:

- Section 1 (before-analysis summary): 10% — pass/fail; either
  they ran the analyzer or they didn't.
- Section 2 (prioritization): 35% — the most important section.
  Look for *defensible* ranking with explicit criteria, not for
  matching the answer key exactly. There is no single right
  answer; there are answers that demonstrate reasoning and
  answers that don't.
- Section 3 (patch): 20% — does the fix actually address the
  issue, is the code clean, did it compile.
- Section 4 (after-analysis summary): 10% — did they notice
  whether the analyzer changed; can they explain mismatches
  between expected and actual changes.
- Section 5 (reflection): 25% — this section tells you whether
  the student understood the lesson. The "what did you notice
  that the analyzer didn't catch" prompt is the centerpiece.

## Common student responses to watch for

**The "all three direct findings" response.** Student picks #1,
#2, #5 — exactly the analyzer's ranking. Their patch fixes the
empty catch. Reflection doesn't mention any invisible issues.
This is the response that means Lab 3 didn't land. Worth a
follow-up conversation rather than a low grade.

**The "one invisible, two direct" response.** Student notices
#7 (batch hang) but otherwise tracks the analyzer ranking. This
is solid — they're operating at the right level for one issue.
Probe in feedback whether they considered #6 and #8.

**The "ranked by severity, not by visibility" response.** Student
prioritizes #3 (EmailGateway timeout), #7 (batch hang), #8
(stale fallback) — none of which are direct findings. Their
reflection explicitly addresses what the analyzer missed. This
is the response Lab 3 is designed to elicit. Recognize it.

**The "analyzer-is-wrong" response.** Student writes a long
critique of the analyzer's limitations and concludes it's not
useful. This overshoots — the analyzer IS useful, it's just not
complete. The right frame is "starting point, not verdict."
Worth pushing back on in feedback.

## Pacing suggestions

Lab 3 is designed for ~90 minutes of in-class work plus a take-home
reflection of similar length. Suggested allocation:

- Read codebase and run analyzer (15 min)
- Prioritization (Sections 1-2 of the worksheet) (30 min)
- Patch one issue (Section 3) (25 min)
- Re-run analyzer, write reflection (Sections 4-5) (20 min)

Students who finish the patch quickly can attempt a second fix.
Students who get stuck on the patch should be encouraged to
articulate the fix in prose without implementing it — that's
still valuable Section 3 content.
