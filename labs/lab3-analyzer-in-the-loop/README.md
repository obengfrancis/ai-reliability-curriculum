# Lab 3 — Analyzer in the Loop

In this lab you run the reliability analyzer over a medium-scale Java
service, decide which problems matter most, fix one of them, and
articulate a hardening plan for the rest.

Lab 1 taught you to **critique** AI-generated code. Lab 2 taught you
to **implement** fault-tolerance patterns. Lab 3 puts you in the role
of a **designer**: there is more code than you can fix in one
session, the analyzer surfaces some — but not all — of what's wrong,
and you have to make prioritization decisions and justify them.

## What's in this codebase

A medium-scale user-profile service in roughly 10 Java files (~600
lines total):

```
edu.example.profileservice
├── api/                ← top-level entry points
│   ├── ProfileApi.java
│   └── BatchApi.java
├── core/               ← business logic
│   ├── ProfileService.java
│   ├── CacheManager.java
│   └── NotificationDispatcher.java
├── storage/            ← persistence
│   ├── ProfileRepository.java
│   └── CacheStore.java
├── external/           ← outbound calls
│   ├── EmailGateway.java
│   └── AuditLogger.java
└── Main.java           ← demo runner
```

The code compiles, runs the happy path on the included demo, and
looks reasonable on first read. It also contains reliability
concerns of varying severity, in different places, and of different
analyzer visibility. Your job is to find them and prioritize.

## Build and run

Requirements: JDK 17 or later, Maven 3.6+, Python 3 (for the
analyzer wrapper). See `BUILD.md` for troubleshooting.

```bash
mvn compile         # build the classes
mvn exec:java       # run the demo against the in-process API
```

The demo prints a few lines per public API to show everything is
wired correctly. Lab 3 is **not** about finding failures at
runtime — it's about reasoning from the source code.

## Run the analyzer

```bash
python3 analyzer_report.py
```

This runs the reliability analyzer (the same Section 3.2 audit
script you encountered in lecture) over the Lab 3 codebase and
prints a per-file summary plus a "files to investigate first"
ranking based on what the analyzer can directly detect.

If your analyzer wrapper can't find the audit runner, pass its
path explicitly:

```bash
python3 analyzer_report.py --audit-runner ../../audit/audit_runner.py
```

## The worksheet

Open `lab3_reflection_template.md` and complete the five sections:

1. **Before-analysis summary.** Paste or summarize the analyzer's
   top findings.
2. **Prioritization.** Choose the three most critical issues and
   explain *why*. Your three may or may not match the analyzer's
   ranking — the analyzer doesn't see everything.
3. **Patch.** Pick one of the three and fix it. Describe what you
   changed and what the analyzer now shows.
4. **After-analysis summary.** Re-run `analyzer_report.py`. Note
   what changed.
5. **Reflection.** What did the analyzer reveal that you would have
   missed? What did *you* notice that the analyzer didn't catch?
   What's your hardening plan for the remaining issues?

The reflection is short — about 1-2 pages including the analyzer
output excerpts.

## A note on what the analyzer can and cannot see

The analyzer is regex-based: it counts try blocks, empty catches,
generic catches, propagation issues, and the presence (or absence)
of recovery patterns like retry, timeout, and circuit breaker. It
does *not* see:

- Resource leaks that arise from missing `finally` blocks or
  try-with-resources statements.
- Asynchronous calls without per-operation timeouts.
- Silent fallback behaviors that hide failures from callers.
- Whether a present pattern is *enough* for the operation it wraps
  (e.g., a timeout on a call that also needs retry).

A reliability concern that the analyzer doesn't see is not less
serious — it is harder to find. Some of the most important issues
in this codebase live in files the analyzer flags as clean. The
worksheet asks you to take that seriously.

## What you'll submit

- This worksheet (`lab3_reflection_template.md`), filled in.
- Your patch (the actual file you modified, or a diff).
- Before and after analyzer outputs (paste excerpts into the
  worksheet; full output is not required).

## What you will NOT do in this lab

You will not implement retry helpers or circuit breakers from
scratch — that's Lab 2's job. If your chosen fix needs one of
those patterns, you may use a real library (e.g., Resilience4j) or
the implementations you wrote in Lab 2. The point of Lab 3 is
designer-level *judgment*, not pattern construction.
