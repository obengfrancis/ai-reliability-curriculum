#!/usr/bin/env python3
"""
Inter-rater reliability for the audit validation study.

Computes, per indicator:
  - percent agreement (always defined)
  - Gwet's AC1 (robust to high-prevalence skew; correct for the all-zero indicators)
  - Cohen/Conger kappa (reported only where it is well-defined, i.e. both coders
    show variance; undefined/unstable on constant indicators by design)

Usage:
  python3 compute_irr.py coder1.csv coder2.csv [--out irr_results.csv]

Both CSVs must have one row per sample (same sample order) and these columns:
  has_try_block, try_blocks, empty_catches, generic_catches, has_retry,
  has_timeout, has_circuit_breaker, has_backoff, recovery_present,
  propagation_violations
(If your column names carry a prefix like 'manual_' or 'coder2_', the loader
strips a leading 'manual_'/'coder1_'/'coder2_' so the two files line up.)
"""
import sys, argparse, warnings
import pandas as pd
from irrCAC.raw import CAC

warnings.filterwarnings("ignore")  # silence the divide-by-zero on constant indicators

INDICATORS = [
    "has_try_block", "try_blocks", "empty_catches", "generic_catches",
    "has_retry", "has_timeout", "has_circuit_breaker", "has_backoff",
    "recovery_present", "propagation_violations",
]

PREFIXES = ("manual_", "coder1_", "coder2_", "script_", "auto_")

def normalize(df):
    cols = {}
    for c in df.columns:
        name = c
        for p in PREFIXES:
            if name.startswith(p):
                name = name[len(p):]
                break
        cols[c] = name
    return df.rename(columns=cols)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("coder1")
    ap.add_argument("coder2")
    ap.add_argument("--out", default="irr_results.csv")
    args = ap.parse_args()

    c1 = normalize(pd.read_csv(args.coder1))
    c2 = normalize(pd.read_csv(args.coder2))

    rows = []
    for ind in INDICATORS:
        if ind not in c1.columns or ind not in c2.columns:
            print(f"  [skip] indicator '{ind}' missing from one file")
            continue
        a = c1[ind].reset_index(drop=True)
        b = c2[ind].reset_index(drop=True)

        # align length defensively
        n = min(len(a), len(b))
        a, b = a[:n], b[:n]

        pa = (a == b).mean() * 100.0
        ratings = pd.DataFrame({"c1": a, "c2": b})
        cac = CAC(ratings)

        # Gwet AC1 — always defined
        try:
            ac1 = cac.gwet()["est"]["coefficient_value"]
        except Exception:
            ac1 = float("nan")

        # Cohen/Conger kappa — undefined when an indicator is constant
        constant = (a.nunique() == 1) and (b.nunique() == 1)
        if constant:
            kappa = "undefined (constant)"
        else:
            try:
                kappa = round(cac.conger()["est"]["coefficient_value"], 3)
            except Exception:
                kappa = "undefined"

        rows.append({
            "indicator": ind,
            "percent_agreement": round(pa, 1),
            "gwet_ac1": round(ac1, 3) if ac1 == ac1 else "nan",
            "cohen_kappa": kappa,
            "distribution": "constant(all-0)" if constant else "variable",
        })

    out = pd.DataFrame(rows)
    print(out.to_string(index=False))
    out.to_csv(args.out, index=False)

    # Aggregate line: overall percent agreement and mean AC1
    overall_pa = out["percent_agreement"].mean()
    ac1_vals = pd.to_numeric(out["gwet_ac1"], errors="coerce")
    print()
    print(f"Overall mean percent agreement across indicators: {overall_pa:.1f}%")
    print(f"Mean Gwet AC1 across indicators: {ac1_vals.mean():.3f}")
    print(f"Total decisions: {len(out)} indicators x N samples")

if __name__ == "__main__":
    main()
