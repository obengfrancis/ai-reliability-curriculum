#!/usr/bin/env python3
"""
analyzer_report.py
==================
Run the Section 3.2 audit runner over the Lab 3 codebase and produce
a human-readable summary suitable for the worksheet.

The audit runner (../../audit/audit_runner.py) scans a flat directory
of *.java files. Lab 3 is organized in nested packages, so this
wrapper:

  1. Discovers all .java source files under src/main/java/.
  2. Copies them into a temporary flat directory.
  3. Generates a stub samples_log.csv keyed by file basename.
  4. Invokes the audit runner with these inputs.
  5. Parses the resulting audit_results.csv and prints a
     prioritization-friendly report: per-file findings, overall
     totals, and a "files to investigate first" list ranked by
     direct analyzer signals.

Usage
-----
    python3 analyzer_report.py                  # default paths
    python3 analyzer_report.py --csv out.csv    # also write CSV
    python3 analyzer_report.py --audit-runner /path/to/audit_runner.py
"""

from __future__ import annotations

import argparse
import csv
import shutil
import subprocess
import sys
import tempfile
from collections import OrderedDict
from pathlib import Path

LAB3_ROOT = Path(__file__).resolve().parent
DEFAULT_AUDIT_RUNNER = LAB3_ROOT.parent.parent / "audit" / "audit_runner.py"
DEFAULT_SRC_DIR = LAB3_ROOT / "src" / "main" / "java"

# Issues that the audit runner can directly detect (analyzer-visible)
DIRECT_INDICATORS = {
    "empty_catches": "Empty catch block (no recovery logic)",
    "generic_catches": "Generic catch (Exception / Throwable / RuntimeException)",
    "propagation_violations": "Potential propagation issue (caught variable not in throw)",
}

# Recovery patterns: presence is good news; absence is interpretive
RECOVERY_INDICATORS = ["has_retry", "has_timeout", "has_circuit_breaker", "has_backoff"]


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Run the audit analyzer over the Lab 3 codebase.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--src-dir", type=Path, default=DEFAULT_SRC_DIR,
                   help=f"Java source root (default: {DEFAULT_SRC_DIR})")
    p.add_argument("--audit-runner", type=Path, default=DEFAULT_AUDIT_RUNNER,
                   help=f"Path to audit_runner.py (default: {DEFAULT_AUDIT_RUNNER})")
    p.add_argument("--csv", type=Path, default=None,
                   help="Optional path to write a copy of the audit CSV.")
    return p.parse_args()


def discover_sources(src_dir: Path) -> list[Path]:
    """Recursively find all .java files under src_dir."""
    if not src_dir.is_dir():
        sys.exit(f"Source directory not found: {src_dir}")
    files = sorted(src_dir.rglob("*.java"))
    if not files:
        sys.exit(f"No .java files found under {src_dir}")
    return files


def stage_for_runner(files: list[Path], work_dir: Path) -> tuple[Path, Path]:
    """Copy files into a flat directory and synthesize a samples log.

    Returns (flat_samples_dir, samples_log_csv).
    Uses dotted package + class as the sample_id so the report
    preserves where each file lived.
    """
    samples = work_dir / "samples"
    samples.mkdir()
    log_path = work_dir / "samples_log.csv"

    rows = []
    for fp in files:
        # Build sample_id from the relative path: java/edu/example/foo/Bar.java
        # → edu.example.foo.Bar
        try:
            rel = fp.relative_to(fp.parents[len(fp.parents) - 1])  # absolute fallback
        except ValueError:
            rel = fp
        # Walk up to the "java" directory and slice the package part below it.
        parts = list(fp.parts)
        try:
            java_idx = parts.index("java")
            pkg_parts = parts[java_idx + 1:-1]
        except ValueError:
            pkg_parts = []
        sample_id = ".".join(pkg_parts + [fp.stem]) if pkg_parts else fp.stem

        shutil.copy2(fp, samples / f"{sample_id}.java")
        rows.append({
            "sample_id": sample_id,
            "prompt": "lab3",
            "model": "lab3-codebase",
            "model_version": "1.0",
            "date": "2026-06-21",
            "compiles": "yes",
        })

    with open(log_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f, fieldnames=["sample_id", "prompt", "model",
                           "model_version", "date", "compiles"])
        writer.writeheader()
        writer.writerows(rows)

    return samples, log_path


def run_audit(audit_runner: Path, samples_dir: Path, log: Path, work_dir: Path) -> Path:
    """Invoke audit_runner.py and return the path to its output CSV."""
    output_csv = work_dir / "audit_results.csv"
    cmd = [
        sys.executable, str(audit_runner),
        "--samples-dir", str(samples_dir),
        "--log", str(log),
        "--output", str(output_csv),
        "--quiet",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        sys.stderr.write("audit_runner.py failed:\n")
        sys.stderr.write(result.stderr)
        sys.exit(result.returncode)
    if not output_csv.exists():
        sys.exit("audit_runner.py did not produce an output CSV")
    return output_csv


def read_results(csv_path: Path) -> list[dict]:
    with open(csv_path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def to_int(v) -> int:
    try:
        return int(v)
    except (TypeError, ValueError):
        try:
            return int(float(v))
        except (TypeError, ValueError):
            return 0


def to_bool(v) -> bool:
    if isinstance(v, bool):
        return v
    return str(v).strip().lower() in {"true", "1", "yes"}


def print_report(rows: list[dict]) -> None:
    """Print the human-readable analyzer report."""
    n = len(rows)
    print("=" * 72)
    print(f"  ANALYZER REPORT — Lab 3 codebase  ({n} source files)")
    print("=" * 72)

    # Per-file table
    print("\nPer-file findings:")
    print(f"  {'file':<50} {'try':>4} {'empty':>6} {'gen':>5} {'prop':>5} {'recov':>6}")
    print(f"  {'-' * 50} {'----':>4} {'-----':>6} {'---':>5} {'----':>5} {'-----':>6}")
    for r in rows:
        sid = r.get("sample_id", "")
        print(f"  {sid:<50} "
              f"{to_int(r.get('try_blocks')):>4} "
              f"{to_int(r.get('empty_catches')):>6} "
              f"{to_int(r.get('generic_catches')):>5} "
              f"{to_int(r.get('propagation_violations')):>5} "
              f"{('yes' if to_bool(r.get('recovery_present')) else 'no'):>6}")
        
    print()
    print('  Note on recov: "yes" means the analyzer detected at least one')
    print("  recovery-related idiom, such as a timeout, retry, backoff, or")
    print("  circuit-breaker pattern. It does not mean the file is reliable,")
    print("  and it does not mean the right recovery pattern was used for the")
    print("  context.")
    print()   

    # Totals
    print("\nTotals across the codebase:")
    totals = OrderedDict([
        ("Empty catches", sum(to_int(r.get("empty_catches")) for r in rows)),
        ("Generic catches", sum(to_int(r.get("generic_catches")) for r in rows)),
        ("Propagation issues", sum(to_int(r.get("propagation_violations")) for r in rows)),
        ("Files with any try block", sum(1 for r in rows if to_bool(r.get("has_try_block")))),
        ("Files with recovery pattern", sum(1 for r in rows if to_bool(r.get("recovery_present")))),
        ("Files with retry pattern", sum(1 for r in rows if to_bool(r.get("has_retry")))),
        ("Files with timeout pattern", sum(1 for r in rows if to_bool(r.get("has_timeout")))),
        ("Files with circuit-breaker pattern",
         sum(1 for r in rows if to_bool(r.get("has_circuit_breaker")))),
    ])
    for label, value in totals.items():
        print(f"  {label:<40} : {value}")

    # Files-to-investigate ranking based on DIRECT signals only
    print("\nFiles to investigate first (ranked by analyzer-detected issues):")
    print("  Note: this ranking uses ONLY the analyzer's directly-detectable")
    print("  indicators. Reliability issues that require human judgment")
    print("  (resource leaks, missing per-call timeouts in parallel loops,")
    print("  silent fallback to stale data) are NOT reflected here. The")
    print("  analyzer is a starting point, not a verdict.\n")

    def issue_count(r):
        return (to_int(r.get("empty_catches"))
                + to_int(r.get("generic_catches"))
                + to_int(r.get("propagation_violations")))

    ranked = sorted(rows, key=lambda r: (-issue_count(r), r.get("sample_id", "")))
    for r in ranked:
        cnt = issue_count(r)
        if cnt == 0:
            continue
        sid = r.get("sample_id", "")
        flags = []
        if to_int(r.get("empty_catches")):
            flags.append(f"{to_int(r.get('empty_catches'))} empty")
        if to_int(r.get("generic_catches")):
            flags.append(f"{to_int(r.get('generic_catches'))} generic")
        if to_int(r.get("propagation_violations")):
            flags.append(f"{to_int(r.get('propagation_violations'))} propagation")
        print(f"  {sid:<50} ({', '.join(flags)})")

    files_no_direct = [r for r in rows if issue_count(r) == 0]
    if files_no_direct:
        print("\nFiles with no direct analyzer findings:")
        for r in files_no_direct:
            print(f"  {r.get('sample_id', '')}")
        print("\n  These are the files where reliability concerns, if any,")
        print("  require reading the code. The worksheet asks you to consider")
        print("  whether some of your three most critical issues live here.")
    print()


def main() -> int:
    args = parse_args()

    if not args.audit_runner.is_file():
        sys.exit(f"audit_runner.py not found at {args.audit_runner}\n"
                 f"Pass --audit-runner /path/to/audit_runner.py")

    files = discover_sources(args.src_dir)
    with tempfile.TemporaryDirectory(prefix="lab3-analyzer-") as wd:
        work_dir = Path(wd)
        samples_dir, log = stage_for_runner(files, work_dir)
        output_csv = run_audit(args.audit_runner, samples_dir, log, work_dir)
        rows = read_results(output_csv)
        if args.csv:
            shutil.copy2(output_csv, args.csv)
        print_report(rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
