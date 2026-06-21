#!/usr/bin/env python3
"""
audit_runner.py
================
Per-sample reliability metrics for the AI-generated-code audit accompanying
the SIGCSE PCI submission (Section 3.2). Walks a directory of standalone
Java samples, computes the audit's five metrics, joins with the samples
log, and prints aggregates ready to fill Table 1.

This script REUSES NiP's regex resilience-pattern detection
(detect_java_resilience_patterns_fallback) and OPTIONALLY invokes NiP's
JavaParser JAR for basic/advanced classification. It is a standalone
companion to WebServFH.py, not a modification of it: the SANER pipeline
is left untouched.

Metrics (per file)
------------------
1. try_blocks                  - count of `try {` and try-with-resources
2. empty_catches               - catch blocks whose body is whitespace/comments only
3. generic_catches             - catches of Exception / Throwable / RuntimeException
4. propagation_violations      - `throw new X(...)` inside a catch whose body
                                 does not mention the caught variable in
                                 the throw's argument list. Detects the
                                 most common destructive-wrapping pattern;
                                 borderline cases (e.g., passing only
                                 `e.getMessage()`, which still loses the
                                 cause chain) are NOT separated out.
                                 Report this as "potential propagation-
                                 discipline issues" rather than as strict
                                 violations.
5. recovery_present            - any of: retry, timeout, circuit_breaker, backoff
                                 (NiP fallback + modern Java HttpClient timeouts)

Notes on scope
--------------
* The audit's published "exception-coverage ratio" requires per-call AST
  analysis with JDK signature knowledge to compute robustly. This script
  reports two defensible proxies instead - try-block presence (% of samples
  with at least one try) and mean try-block count per sample - which Section
  3.2's Table 1 should reflect.
* "Fallback" as a recovery sub-pattern is NOT regex-detectable in general
  and is excluded from recovery_present. retry / timeout / circuit_breaker /
  backoff are detected.
* All count metrics are scoped per file. Catch-body extraction uses brace
  matching that skips comments and string literals; however, the
  initial try/catch regex scans the raw source, so spurious matches
  inside `// ... try {` comments or `"... catch (...) {"` string literals
  are possible in principle. In practice this is rare for AI-generated
  samples, but results should be manually spot-checked on a subsample.

Usage
-----
    python audit_runner.py \\
        --samples-dir samples/ \\
        --log samples_log.csv \\
        --output audit_results.csv \\
        [--jar target/your-artifact-id-1.0-SNAPSHOT.jar]

The samples log CSV must contain at least: sample_id, prompt, model,
model_version, date, compiles. Files in samples-dir are matched on
basename (without .java extension) to sample_id.
"""

from __future__ import annotations

import argparse
import csv
import json
import logging
import os
import re
import subprocess
import sys
import tempfile
from collections import defaultdict
from pathlib import Path
from statistics import mean
from typing import Dict, List


# ---------------------------------------------------------------------------
#  Compiled regex patterns
# ---------------------------------------------------------------------------

# try { ... }  or  try (resource = ...) { ... }
TRY_BLOCK_RE = re.compile(r"\btry\s*[\{\(]")

# catch header: captures type list (for multi-catch) and the variable name
CATCH_HEADER_RE = re.compile(
    r"\bcatch\s*\(\s*(?:final\s+)?"
    r"([\w\.]+(?:\s*\|\s*[\w\.]+)*)"   # group 1: types (possibly A | B | C)
    r"\s+(\w+)\s*\)\s*\{"               # group 2: variable name
)

# Types treated as "generic" (overly-broad catches)
GENERIC_TYPES = {"Exception", "Throwable", "RuntimeException"}

# Modern Java timeout signals not in NiP's fallback (Resilience4j-focused)
MODERN_TIMEOUT_RES = [
    re.compile(r"\.connectTimeout\s*\("),     # HttpClient.Builder
    re.compile(r"\.timeout\s*\(\s*Duration\."),  # HttpRequest.Builder
    re.compile(r"\bsetConnectTimeout\s*\("),  # HttpURLConnection
    re.compile(r"\bsetReadTimeout\s*\("),     # HttpURLConnection
    # Concurrent / executor timeouts. Added after manual validation of
    # parallel_copilot_04 surfaced awaitTermination as a missed case;
    # documented in Section 3.2's methodology paragraph.
    re.compile(r"\bawaitTermination\s*\("),       # ExecutorService bounded wait
    re.compile(r"\.get\s*\([^)]*\bTimeUnit\."),   # Future.get(timeout, TimeUnit)
]

# `throw new X(args);`  - args captured for cause-propagation check
THROW_NEW_RE = re.compile(
    r"\bthrow\s+new\s+\w+\s*\(([^;]*?)\)\s*;",
    re.DOTALL,
)


# ---------------------------------------------------------------------------
#  Brace-matched body extraction
# ---------------------------------------------------------------------------

def find_block_end(code: str, open_brace_idx: int) -> int:
    """Return the index of the '}' matching the '{' at open_brace_idx.

    Tracks brace depth while ignoring braces inside string literals, char
    literals, line comments, and block comments. Returns -1 if no match
    found (malformed input).
    """
    depth = 0
    i = open_brace_idx
    n = len(code)
    in_str = False
    str_quote = ""
    in_line_comment = False
    in_block_comment = False

    while i < n:
        c = code[i]
        nxt = code[i + 1] if i + 1 < n else ""

        if in_line_comment:
            if c == "\n":
                in_line_comment = False
        elif in_block_comment:
            if c == "*" and nxt == "/":
                in_block_comment = False
                i += 1
        elif in_str:
            if c == "\\":
                i += 1  # skip escaped char
            elif c == str_quote:
                in_str = False
        else:
            if c == "/" and nxt == "/":
                in_line_comment = True
                i += 1
            elif c == "/" and nxt == "*":
                in_block_comment = True
                i += 1
            elif c == '"' or c == "'":
                in_str = True
                str_quote = c
            elif c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    return i
        i += 1

    return -1


def strip_comments(text: str) -> str:
    """Remove // line comments and /* */ block comments from a snippet."""
    text = re.sub(r"//[^\n]*", "", text)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return text


# ---------------------------------------------------------------------------
#  Recovery-pattern detection (NiP fallback + modern Java timeouts)
# ---------------------------------------------------------------------------

def detect_recovery_patterns(code: str) -> Dict[str, bool]:
    """Adapted from NiP's detect_java_resilience_patterns_fallback.
    Extended with modern Java HttpClient / HttpURLConnection timeout
    detection, since NiP's fallback focuses on Resilience4j / Spring /
    Hystrix idioms that audit samples are unlikely to use.
    """
    patterns = {
        "retry": False,
        "circuit_breaker": False,
        "timeout": False,
        "exponential_backoff": False,
    }

    def has_ann(name: str) -> bool:
        return bool(re.search(rf"@{name}\b", code))

    # Annotations
    if has_ann("Retryable") or has_ann("Retry"):
        patterns["retry"] = True
    if has_ann("HystrixCommand") or has_ann("CircuitBreaker"):
        patterns["circuit_breaker"] = True
    if has_ann("TimeLimiter"):
        patterns["timeout"] = True
    if has_ann("Backoff"):
        patterns["exponential_backoff"] = True

    # Class names
    if re.search(r"\b(RetryConfig|RetryRegistry|RetryTemplate)\b", code):
        patterns["retry"] = True
    if re.search(r"\b(CircuitBreakerConfig|CircuitBreakerRegistry|HystrixCommand)\b", code):
        patterns["circuit_breaker"] = True
    if re.search(r"\b(TimeLimiterConfig|TimeLimiterRegistry)\b", code):
        patterns["timeout"] = True
    if re.search(r"\b(ExponentialBackOff|BackOffPolicy)\b", code):
        patterns["exponential_backoff"] = True

    # Calls
    if re.search(r"\bRetry\.of\(|\.(retry|withRetry)\(", code):
        patterns["retry"] = True
    if re.search(r"\bCircuitBreaker\.of\(|\.circuitBreaker\(|withCircuitBreaker\(", code):
        patterns["circuit_breaker"] = True
    if re.search(r"\bretryTemplate\.execute\(", code):
        patterns["retry"] = True

    # Modern Java HTTP timeouts (NEW for audit)
    for rx in MODERN_TIMEOUT_RES:
        if rx.search(code):
            patterns["timeout"] = True
            break

    return patterns


# ---------------------------------------------------------------------------
#  Per-file analysis
# ---------------------------------------------------------------------------

def analyze_java_file(code: str) -> Dict:
    """Compute all per-file metrics for one Java sample."""
    try_count = len(TRY_BLOCK_RE.findall(code))

    total_catches = 0
    empty_catches = 0
    generic_catches = 0
    propagation_violations = 0

    for m in CATCH_HEADER_RE.finditer(code):
        total_catches += 1
        types_part = m.group(1)
        var_name = m.group(2)

        # Multi-catch: any type in the alternation counts as "generic"
        # (catch (IOException | Exception e) is still generic).
        catch_types = [
            t.strip().rsplit(".", 1)[-1]
            for t in types_part.split("|")
        ]
        if any(t in GENERIC_TYPES for t in catch_types):
            generic_catches += 1

        # Extract catch body via brace matching
        open_brace = code.find("{", m.start())
        if open_brace < 0:
            continue
        close_brace = find_block_end(code, open_brace)
        if close_brace < 0:
            continue
        body = code[open_brace + 1:close_brace]

        # Empty? (whitespace and comments only)
        if not strip_comments(body).strip():
            empty_catches += 1

        # Destructive wrapping: throw new X(...) without caught variable as arg
        body_no_comments = strip_comments(body)
        for tm in THROW_NEW_RE.finditer(body_no_comments):
            args = tm.group(1)
            if not re.search(rf"\b{re.escape(var_name)}\b", args):
                propagation_violations += 1

    recovery = detect_recovery_patterns(code)

    return {
        "try_blocks": try_count,
        "has_try_block": try_count > 0,
        "total_catches": total_catches,
        "empty_catches": empty_catches,
        "generic_catches": generic_catches,
        "propagation_violations": propagation_violations,
        "has_retry": recovery["retry"],
        "has_timeout": recovery["timeout"],
        "has_circuit_breaker": recovery["circuit_breaker"],
        "has_backoff": recovery["exponential_backoff"],
        "recovery_present": any(recovery.values()),
    }


# ---------------------------------------------------------------------------
#  Optional JAR invocation
# ---------------------------------------------------------------------------

def invoke_jar(code: str, jar_path: Path) -> Dict:
    """Call NiP's JavaParser JAR for basic/advanced classification.
    Returns {} on any failure - the regex metrics still stand alone.
    """
    tmp = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", suffix=".java", delete=False, encoding="utf-8"
        ) as f:
            f.write(code)
            tmp = f.name
        result = subprocess.run(
            ["java", "-jar", str(jar_path), tmp],
            capture_output=True, text=True, timeout=30,
        )
        if result.returncode != 0:
            return {}
        out = json.loads(result.stdout)
        return {
            "jar_has_basic": bool(out.get("hasBasicHandling", False)),
            "jar_has_advanced": bool(out.get("hasAdvancedHandling", False)),
        }
    except Exception as e:
        logging.warning(f"JAR invocation failed: {e}")
        return {}
    finally:
        if tmp and os.path.exists(tmp):
            try:
                os.remove(tmp)
            except OSError:
                pass


# ---------------------------------------------------------------------------
#  Samples log
# ---------------------------------------------------------------------------

def load_samples_log(log_path: Path) -> Dict[str, Dict]:
    """Read samples_log.csv keyed by sample_id."""
    log = {}
    with open(log_path, newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            sid = (row.get("sample_id") or "").strip()
            if sid:
                log[sid] = row
    return log


# ---------------------------------------------------------------------------
#  Aggregates
# ---------------------------------------------------------------------------

def print_aggregates(results: List[Dict]) -> None:
    """Print Table 1 numbers - overall and per-model."""
    n = len(results)

    def pct(rows, key):
        return 100.0 * sum(1 for r in rows if r[key]) / max(len(rows), 1)

    def avg(rows, key):
        vals = [r[key] for r in rows if isinstance(r[key], (int, float))]
        return mean(vals) if vals else 0.0

    def block(rows):
        return (
            f"  try-block presence (%)         : {pct(rows, 'has_try_block'):6.1f}\n"
            f"  try blocks (mean)              : {avg(rows, 'try_blocks'):6.2f}\n"
            f"  empty catches (mean)           : {avg(rows, 'empty_catches'):6.2f}\n"
            f"  generic catches (mean)         : {avg(rows, 'generic_catches'):6.2f}\n"
            f"  recovery present (%)           : {pct(rows, 'recovery_present'):6.1f}\n"
            f"    has_retry (%)                : {pct(rows, 'has_retry'):6.1f}\n"
            f"    has_timeout (%)              : {pct(rows, 'has_timeout'):6.1f}\n"
            f"    has_circuit_breaker (%)      : {pct(rows, 'has_circuit_breaker'):6.1f}\n"
            f"    has_backoff (%)              : {pct(rows, 'has_backoff'):6.1f}\n"
            f"  propagation violations (mean)  : {avg(rows, 'propagation_violations'):6.2f}\n"
        )

    print()
    print("=" * 64)
    print(f"  AUDIT SUMMARY  (n = {n} samples)")
    print("=" * 64)
    print("\nOverall:")
    print(block(results))

    by_model = defaultdict(list)
    for r in results:
        by_model[r.get("model", "unknown") or "unknown"].append(r)

    for model in sorted(by_model):
        rows = by_model[model]
        print(f"{model}  (n = {len(rows)}):")
        print(block(rows))

    # Per-prompt cut as well — useful for spotting which task gets the
    # worst handling (typically file I/O or concurrency).
    by_prompt = defaultdict(list)
    for r in results:
        by_prompt[r.get("prompt", "?") or "?"].append(r)
    print("By prompt:")
    print(f"  {'prompt':<8}{'n':>4}  {'try%':>6}  {'recov%':>8}  {'emptyC':>8}  {'genC':>6}  {'prop':>6}")
    for prompt in sorted(by_prompt):
        rows = by_prompt[prompt]
        print(
            f"  {prompt:<8}{len(rows):>4}  "
            f"{pct(rows, 'has_try_block'):>6.1f}  "
            f"{pct(rows, 'recovery_present'):>8.1f}  "
            f"{avg(rows, 'empty_catches'):>8.2f}  "
            f"{avg(rows, 'generic_catches'):>6.2f}  "
            f"{avg(rows, 'propagation_violations'):>6.2f}"
        )
    print()
    print("=" * 64)
    print("  Use the Overall and per-model values to fill Table 1.")
    print("=" * 64)


# ---------------------------------------------------------------------------
#  Main
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Audit AI-generated Java samples for reliability metrics.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--samples-dir", type=Path, default=Path("samples"),
                   help="Directory containing *.java samples (default: samples/)")
    p.add_argument("--log", type=Path, default=Path("samples_log.csv"),
                   help="CSV log keyed by sample_id (default: samples_log.csv)")
    p.add_argument("--output", type=Path, default=Path("audit_results.csv"),
                   help="Per-sample results CSV (default: audit_results.csv)")
    p.add_argument("--jar", type=Path, default=None,
                   help="Optional path to NiP's JavaParser JAR for "
                        "basic/advanced classification.")
    p.add_argument("--quiet", action="store_true",
                   help="Suppress INFO logging.")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    logging.basicConfig(
        level=logging.WARNING if args.quiet else logging.INFO,
        format="%(levelname)s: %(message)s",
    )

    if not args.samples_dir.is_dir():
        logging.error(f"Samples directory not found: {args.samples_dir}")
        return 1
    if not args.log.is_file():
        logging.error(f"Samples log not found: {args.log}")
        return 1

    samples_log = load_samples_log(args.log)
    logging.info(f"Loaded {len(samples_log)} rows from {args.log}")

    use_jar = args.jar is not None and args.jar.exists()
    if args.jar and not use_jar:
        logging.warning(f"JAR not found at {args.jar} - regex-only mode")
    elif use_jar:
        logging.info(f"Using JAR: {args.jar}")

    java_files = sorted(args.samples_dir.glob("*.java"))
    logging.info(f"Found {len(java_files)} .java files")

    # Sanity check: warn on log/file mismatches
    file_ids = {fp.stem for fp in java_files}
    log_ids = set(samples_log.keys())
    only_files = file_ids - log_ids
    only_log = log_ids - file_ids
    if only_files:
        logging.warning(f"{len(only_files)} files have no log row: "
                        f"{sorted(only_files)[:5]}...")
    if only_log:
        logging.warning(f"{len(only_log)} log rows have no file: "
                        f"{sorted(only_log)[:5]}...")

    results: List[Dict] = []
    for fp in java_files:
        sample_id = fp.stem
        try:
            code = fp.read_text(encoding="utf-8", errors="replace")
        except OSError as e:
            logging.error(f"Could not read {fp}: {e}")
            continue

        metrics = analyze_java_file(code)
        if use_jar:
            metrics.update(invoke_jar(code, args.jar))

        log_row = samples_log.get(sample_id, {})
        row = {
            "sample_id": sample_id,
            "prompt": log_row.get("prompt", ""),
            "model": log_row.get("model", ""),
            "model_version": log_row.get("model_version", ""),
            "date": log_row.get("date", ""),
            "compiles": log_row.get("compiles", ""),
        }
        row.update(metrics)
        results.append(row)

    if not results:
        logging.error("No samples analyzed.")
        return 1

    fieldnames = list(results[0].keys())
    with open(args.output, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(results)
    logging.info(f"Wrote {len(results)} rows to {args.output}")

    print_aggregates(results)
    return 0


if __name__ == "__main__":
    sys.exit(main())