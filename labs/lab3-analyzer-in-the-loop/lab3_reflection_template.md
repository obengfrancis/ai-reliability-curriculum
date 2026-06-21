# Lab 3 Worksheet — Analyzer in the Loop

**Name:** _________________________   **Course:** _________________________

---

## 1. Before-analysis summary

Run the analyzer:

```bash
python3 analyzer_report.py
```

Paste the most relevant excerpts below (the totals and the
"files to investigate first" section). You do not need to paste
the entire report.

```
[paste analyzer output here]
```

In 2-3 sentences, what is the analyzer telling you? Where does it
draw your attention first?



---

## 2. Prioritization

Identify the **three most critical reliability risks** in this
codebase. They may come from the analyzer's ranking, from your
own reading of the source, or both.

For each, fill in:

### Issue 1

**Location** (file + method): _____________________________________

**What's wrong:**



**Why is this critical** (impact under failure):



**How visible was this to the analyzer** (directly flagged /
partial signal / not visible — you had to read the code):



### Issue 2

**Location** (file + method): _____________________________________

**What's wrong:**



**Why is this critical** (impact under failure):



**How visible was this to the analyzer** (directly flagged /
partial signal / not visible — you had to read the code):



### Issue 3

**Location** (file + method): _____________________________________

**What's wrong:**



**Why is this critical** (impact under failure):



**How visible was this to the analyzer** (directly flagged /
partial signal / not visible — you had to read the code):



**Why these three, and in this order?** (One paragraph. What
criteria did you use to rank? What did you weight more heavily —
failure severity, blast radius, frequency, or something else?)



---

## 3. Patch

Pick one of your three issues — typically the highest-priority
one — and fix it. Briefly describe:

**Which issue you chose, and why this one first:**



**What you changed** (a few sentences, no full diff required —
the patch itself is a separate deliverable):



**What new dependencies, if any, your fix introduced** (e.g.,
Resilience4j, a Lab 2 implementation, a standard library
addition):



---

## 4. After-analysis summary

Re-run the analyzer:

```bash
python3 analyzer_report.py
```

What changed in the analyzer output after your fix?



Did the change you expect to see in the analyzer report match
what you actually saw? If not, what does that tell you?



---

## 5. Reflection

This is the most important section. Spend a paragraph or two on
each prompt.

**What did the analyzer reveal that you would have missed?**
Be specific — name the issue, name the analyzer signal, and
explain what would have happened if you'd skipped the analyzer
step.



**What did you notice that the analyzer did not catch?**
Of your three most critical issues, were any of them invisible to
the analyzer? If so, what kind of reasoning did you have to do to
find them?



**Hardening plan for the remaining issues.**
You fixed one. The other two — plus any other concerns you
identified — still need to be addressed in a future hardening
pass. For each of the other two:

- *Issue:* (file + method)
- *Proposed fix:* (one or two sentences)
- *Priority relative to other work:* (why now or why later)



**What is the role of an analyzer in production?**
Based on this exercise: what is the analyzer good for? What does
it miss? If you were the technical lead on this codebase, how
would you use an analyzer alongside human code review?



---

## Submission

Submit:

- This worksheet, completed.
- Your patch (the modified `.java` file, or a diff).
- Optionally, the before and after CSV files from the analyzer
  (`python3 analyzer_report.py --csv before.csv` etc.).
