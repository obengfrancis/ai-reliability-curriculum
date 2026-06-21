# Fault Analysis Worksheet — Lab 1: "AI Wrote It Wrong"

**Course:** _________________________   **Pair members:** _________________________

---

## Purpose

The `UserProfileService` codebase compiles, runs, and passes a happy-path
demo. Yet it contains four reliability defects that would surface as soon
as the code meets real network conditions, real data, or real failure
modes. Your job is to find them.

For each defect you identify, complete one entry below. Each entry has
five fields. Write 2–4 sentences per field — enough to demonstrate that
you understand the defect, not so much that you reproduce the code.

You are not asked to fix the code in this lab. Lab 2 is where you build
the recovery patterns; Lab 1 is where you learn to see what's missing.

---

## Worked example

The following entry is a model of what a complete defect analysis looks
like. The defect here is one of four — use it as a guide for tone and
detail, then find the other three on your own. Do not copy the entry's
structure so literally that you miss a defect with a different shape.

### Defect — `fetchUserProfile` (lines 50–65): no timeout configured

**Failure scenario.**
The remote profile service becomes slow or hangs (e.g., the server is
overloaded and accepts the TCP connection but never sends a response).
The TCP connection succeeds, so the code does not see a connection
error, but no response data ever arrives.

**Current handling.**
None. The `HttpURLConnection` is created with default settings, and
neither `setConnectTimeout` nor `setReadTimeout` is configured. The
`conn.getInputStream()` call blocks indefinitely waiting for the
server to start sending data.

**Impact.**
The calling thread hangs for as long as the remote server holds the
connection open — potentially minutes or hours. In a web server using
this method to serve user requests, every hung call ties up one request
thread. Under load, this exhausts the thread pool and the entire server
becomes unresponsive, even to requests that have nothing to do with the
profile service.

**Proposed fix.**
Configure `conn.setConnectTimeout(5000)` and `conn.setReadTimeout(10000)`
before opening the connection. These bound the wait at 5 seconds for the
TCP connect and 10 seconds for the response, after which a
`SocketTimeoutException` is thrown and the caller can react.

**Why the AI missed it.**
The prompt asked for "fetches user profile data from a REST API at a
configurable base URL." It described what the method should do on the
happy path but said nothing about latency, hang scenarios, or what
should happen when the server is slow. The model produced code that
satisfies the literal request and stops there. Configuring timeouts
requires reasoning about failure modes that the prompt does not
mention; without that reasoning, the defaults — which are "wait
forever" — go unchallenged.

---

## Your entries

Find the other three defects and complete the entries below. Add a
fourth entry if you also identify the defect demonstrated in the
worked example above as part of your reading — but you do not need to
re-analyze it.

### Defect 1 — _method name_ (lines ___): _short description_

**Failure scenario.**



**Current handling.**



**Impact.**



**Proposed fix.**



**Why the AI missed it.**



---

### Defect 2 — _method name_ (lines ___): _short description_

**Failure scenario.**



**Current handling.**



**Impact.**



**Proposed fix.**



**Why the AI missed it.**



---

### Defect 3 — _method name_ (lines ___): _short description_

**Failure scenario.**



**Current handling.**



**Impact.**



**Proposed fix.**



**Why the AI missed it.**



---

## Discussion prompts (pair share-out)

After completing your three entries, take five minutes to discuss with
your partner:

1. Which defect was hardest to spot, and why?
2. Across the three defects you found, is there a common pattern in
   *why* the AI missed them? What does that pattern suggest about the
   limits of AI-generated code as a starting point for production
   work?
3. If you could change one thing about how the prompt was written to
   the AI, what would it be — and would that change have prevented
   all four defects, or only some?

You will not be assessed on the discussion answers, but they will frame
how Lab 2 introduces the fault-tolerance patterns that close these
gaps.
