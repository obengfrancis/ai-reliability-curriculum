#!/usr/bin/env python3
"""
compare_manual_vs_audit.py
==========================
Joins the manual validation CSV with the script's audit CSV and reports
inter-rater agreement (manual coder vs. audit_runner.py) across the 10
metrics they share. Used to substantiate the manual-validation claim in
Section 3.2 of the PCI paper.

Outputs:
  - Per-metric agreement (e.g., "has_timeout: 24/25 agree")
  - Per-stratum agreement (e.g., "Stratum 1: 4/5 fully agree on all metrics")
  - Overall: number of samples where manual & script agree on ALL metrics
  - A detailed disagreement table (sample, metric, manual, script, file path)
  - Optionally writes the disagreement table to CSV
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

# Metric pairs: (manual_column, script_column, type)
# type: "bool" -> normalize to 0/1; "int" -> int comparison
METRICS = [
    ("manual_has_try_block",          "has_try_block",          "bool"),
    ("manual_try_blocks",             "try_blocks",             "int"),
    ("manual_empty_catches",          "empty_catches",          "int"),
    ("manual_generic_catches",        "generic_catches",        "int"),
    ("manual_has_retry",              "has_retry",              "bool"),
    ("manual_has_timeout",            "has_timeout",            "bool"),
    ("manual_has_circuit_breaker",    "has_circuit_breaker",    "bool"),
    ("manual_has_backoff",            "has_backoff",            "bool"),
    ("manual_recovery_present",       "recovery_present",       "bool"),
    ("manual_propagation_violations", "propagation_violations", "int"),
]


def norm_bool(v: str) -> int:
    """Normalize 0/1, True/False, yes/no -> int 0 or 1."""
    s = (v or "").strip().lower()
    if s in ("1", "true", "yes", "y", "t"):
        return 1
    if s in ("0", "false", "no", "n", "f", ""):
        return 0
    raise ValueError(f"unparseable boolean: {v!r}")


def norm_int(v: str) -> int:
    s = (v or "").strip()
    return int(s) if s else 0


def load(path: Path) -> list[dict]:
    with open(path, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--manual",  default="manual_validation_audit.csv")
    p.add_argument("--audit",   default="audit_results.csv")
    p.add_argument("--samples-dir", default="samples",
                   help="Used only to print file paths in the disagreement list")
    p.add_argument("--out-disagreements", default="disagreements.csv",
                   help="Where to write the long-form disagreement table")
    args = p.parse_args()

    manual_rows = load(Path(args.manual))
    audit_rows  = load(Path(args.audit))
    audit_idx   = {r["sample_id"]: r for r in audit_rows}

    # Join. Skip manual rows that have no audit counterpart.
    paired = []
    for m in manual_rows:
        sid = m["sample_id"]
        if sid not in audit_idx:
            print(f"WARNING: manual sample {sid!r} not in audit; skipping")
            continue
        paired.append((m, audit_idx[sid]))

    n = len(paired)
    if n == 0:
        print("No pairs to compare.")
        return 1

    # Compute agreements
    per_metric_agree = {m_col: 0 for m_col, _, _ in METRICS}
    per_stratum_full_agree = defaultdict(lambda: [0, 0])  # [agreed, total]
    samples_full_agree = 0
    disagreements: list[dict] = []

    for m, a in paired:
        stratum = m.get("selection_stratum", "?")
        per_stratum_full_agree[stratum][1] += 1
        row_all_agree = True

        for m_col, s_col, typ in METRICS:
            try:
                mv = norm_bool(m[m_col]) if typ == "bool" else norm_int(m[m_col])
                sv = norm_bool(a[s_col]) if typ == "bool" else norm_int(a[s_col])
            except (KeyError, ValueError) as e:
                print(f"WARNING: parse error on {m['sample_id']} / {m_col}: {e}")
                continue

            if mv == sv:
                per_metric_agree[m_col] += 1
            else:
                row_all_agree = False
                disagreements.append({
                    "sample_id":  m["sample_id"],
                    "stratum":    stratum,
                    "prompt":     m["prompt"],
                    "model":      m["model"],
                    "metric":     m_col.replace("manual_", ""),
                    "manual":     mv,
                    "script":     sv,
                    "file":       str(Path(args.samples_dir) / f"{m['sample_id']}.java"),
                })

        if row_all_agree:
            samples_full_agree += 1
            per_stratum_full_agree[stratum][0] += 1

    # ----- report -----
    print("\n" + "=" * 64)
    print(f"  Manual-vs-audit agreement  (n = {n} samples)")
    print("=" * 64)

    print("\nPer-metric agreement (count of samples where manual = script):")
    for m_col, _, _ in METRICS:
        a = per_metric_agree[m_col]
        pct = 100.0 * a / n
        bar = "█" * int(round(pct / 5))
        name = m_col.replace("manual_", "")
        print(f"  {name:26s}  {a:>2}/{n}   {pct:5.1f}%   {bar}")

    print("\nPer-stratum FULL agreement (samples where ALL 10 metrics match):")
    for s in sorted(per_stratum_full_agree):
        a, t = per_stratum_full_agree[s]
        pct = 100.0 * a / t if t else 0
        print(f"  Stratum {s}:  {a}/{t}   ({pct:.0f}%)")

    print(f"\nOverall: {samples_full_agree}/{n} samples agree on every metric "
          f"({100.0 * samples_full_agree / n:.1f}%)")

    # Total decisions and overall metric-level accuracy (n samples × 10 metrics)
    total_cells = n * len(METRICS)
    total_agree = sum(per_metric_agree.values())
    print(f"Metric-level accuracy: {total_agree}/{total_cells} "
          f"({100.0 * total_agree / total_cells:.1f}% of all coded decisions)")

    if disagreements:
        print(f"\nDisagreements ({len(disagreements)} total):")
        print(f"  {'sample_id':24s} {'str':3s} {'metric':22s} {'manual':>6s}  {'script':>6s}")
        print(f"  {'-'*24} {'-'*3} {'-'*22} {'-'*6}  {'-'*6}")
        for d in disagreements:
            print(f"  {d['sample_id']:24s}  {d['stratum']}  "
                  f"{d['metric']:22s} {d['manual']!s:>6s}  {d['script']!s:>6s}")
        # write CSV
        with open(args.out_disagreements, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(disagreements[0].keys()))
            w.writeheader()
            w.writerows(disagreements)
        print(f"\nWrote detailed disagreements to {args.out_disagreements}")
    else:
        print("\nNo disagreements.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())