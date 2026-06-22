# Auditing AI-Generated Code for Reliability
This repository is the replication package for a SIGCSE Technical Symposium 2027 
Position and Curricula Initiative paper on teaching software reliability in 
AI-augmented computing courses. It contains the empirical audit motivating the 
curriculum (Section 3.2) and the three scaffolded labs implementing it (Section 4).

## Repository structure

```
.
├── audit/                              ← Section 3.2: reliability audit
│   ├── audit_runner.py                  ← per-sample metrics script
│   ├── prompts.md                       ← the five prompts used
│   ├── samples/                         ← 75 generated Java samples
│   ├── samples_log.csv                  ← per-sample metadata
│   ├── audit_results.csv                ← per-sample metrics output
│   ├── manual_validation_audit.csv      ← 25-sample manual coding
│   └── compare_manual_vs_audit.py       ← validation script
└── labs/                               ← Section 4: the three-lab sequence
    ├── lab1-ai-wrote-it-wrong/          ← critique (high scaffolding)
    ├── lab2-designing-for-recovery/     ← construction (medium)
    └── lab3-analyzer-in-the-loop/       ← refinement (low scaffolding)
```

The labs follow the paper's critic → implementer → designer
progression. Each lab has its own `README.md` describing what
students do, how to run the code, and what they submit.

## Quick start

### "I want to teach this lab"

The student-facing materials are on the `main` branch (this one).
**Answer keys, instructor notes, and reference solutions are on the
`instructor-materials` branch.** To get the full teaching package:

```bash
git clone <repository-url>
cd ai-reliability-curriculum
git checkout instructor-materials
```

Then read `INSTRUCTOR-OVERVIEW.md` on that branch for the full
adoption guide (issue inventories, pacing suggestions, grading
rubrics, reference solutions).

### "I want to take this lab"

Stay on `main`. Pick the lab your instructor assigned:

```bash
cd labs/lab1-ai-wrote-it-wrong   # or lab2-, lab3-
cat README.md
```

Each lab's README is the place to start. The labs are sequenced,
but each is self-contained for build and run purposes.

### "I want to replicate the audit"

```bash
cd audit
python3 audit_runner.py             # processes samples/ via samples_log.csv
```

The samples corpus, the audit script, the per-sample metrics, and
the manual-validation comparison are all in this directory. The
methodology is documented in Section 3.2 of the paper.

## What's available, what's not

Available on this branch (`main`):

- **Audit (Section 3.2):** `audit_runner.py`, the 75-sample corpus,
  per-sample metrics CSV, prompt set, manual-validation CSV, and a
  comparison script.
- **Lab 1 — AI Wrote It Wrong:** It is defective
  `UserProfileService` codebase with four reliability defects, the
  fault analysis worksheet, README, and build notes.
- **Lab 2 — Designing for Recovery:** retry / circuit-breaker
  pattern reference sheet, starter implementations with TODO
  markers, and a JUnit 5 failure-injection test harness (17
  required tests + 3 optional). The harness compiles cleanly
  against the starters and reports instructional failure messages;
  a correct implementation passes all 17 tests.
- **Lab 3 — Analyzer in the Loop:** medium-scale profile service
  codebase (~10 files, ~600 lines) with reliability concerns of
  varying analyzer visibility, a Python wrapper around the
  Section 3.2 audit script that produces a human-readable
  prioritization report, and the student reflection worksheet.

On the `instructor-materials` branch only:

- Lab 1 issue inventory with line numbers and worked worksheet
  entries.
- Lab 2 reference solution (`Retry.java` and `CircuitBreaker.java`)
  validated to make all 17 required tests pass.
- Lab 3 issue inventory with prioritization rationale, expected
  student reasoning per issue, and a grading guide.

## License

This repository is dual-licensed to handle code and text
appropriately:

- **Code** (`.java`, `.py`, `pom.xml`, build files) — MIT License.
  See `LICENSE-CODE`.
- **Documentation, worksheets, and instructor notes** (`.md` files)
  — Creative Commons Attribution 4.0 International (CC BY 4.0).
  See `LICENSE-TEXT`.

Adopters are encouraged to use, modify, and redistribute these
materials. Attribution to the paper (see Citation below) is
required for the text materials and appreciated for the code.

## Citation

> **For the double-blind review version:** the citation below is
> currently in anonymized form. After acceptance and publication,
> we will replace with the
> post-publication BibTeX block in `CITATION-POST-PUBLICATION.bib`
> (provided as a template in this repository).

If you use these materials in teaching or research, please cite:

> Anonymous Authors (2027). *From Reliability Blind Spots to
> Adoptable Classroom Practice: Teaching Software Engineering for
> Reliability in the Age of AI.* In Proceedings of the 58th ACM
> Technical Symposium on Computer Science Education (SIGCSE TS '27).

A BibTeX template for the post-publication citation is in
`CITATION-POST-PUBLICATION.bib`; we will replace the placeholder fields
once the paper is accepted and published.

## Reporting issues, suggesting changes

This repository is the artifact accompanying a research paper. For
errata, replication concerns, or suggestions, open an issue on this
repository. We welcome contributions from instructors adopting
these materials in their own courses.

