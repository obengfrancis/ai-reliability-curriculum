# Instructor Overview

You are on the `instructor-materials` branch of the
ai-reliability-curriculum repository. This branch contains
everything on `main` (the student-facing materials) **plus** the
answer keys and reference solutions that should not be visible to
students.

If a student or reviewer accidentally found their way here, switch
back to `main`:

```bash
git checkout main
```

## What's on this branch that's NOT on main

```
labs/
├── lab1-ai-wrote-it-wrong/
│   └── instructor_notes.md           ← Lab 1 issue inventory
├── lab2-designing-for-recovery/
│   └── solution/                     ← reference Retry + CircuitBreaker
│       ├── Retry.java
│       ├── CircuitBreaker.java
│       └── README.md                 ← validation workflow
└── lab3-analyzer-in-the-loop/
    └── instructor_notes.md           ← Lab 3 issue inventory + grading guide
```

## How to use the answer keys

### Lab 1 instructor notes

`labs/lab1-ai-wrote-it-wrong/instructor_notes.md` catalogs the four
reliability defects in `UserProfileService.java` with line numbers,
worked worksheet entries, demo instructions, and "why the AI missed
it" framings for class discussion. Use as the answer key for
grading and as a discussion script for the post-lab debrief.

### Lab 2 reference solution

`labs/lab2-designing-for-recovery/solution/` contains the reference
implementation of `Retry.withBackoff(...)` and `CircuitBreaker.call(...)`.

To validate the harness on your machine, swap the reference files
into place temporarily:

```bash
cd labs/lab2-designing-for-recovery
# Back up the starters
cp src/main/java/edu/example/userprofile/resilience/Retry.java /tmp/Retry-starter.java
cp src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java /tmp/CircuitBreaker-starter.java
# Swap in the reference
cp solution/Retry.java src/main/java/edu/example/userprofile/resilience/Retry.java
cp solution/CircuitBreaker.java src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java
# Verify
mvn test                # expect: Tests run: 17, Failures: 0
mvn test -P optional    # expect: Tests run: 3, Failures: 0
# Restore the starters
cp /tmp/Retry-starter.java src/main/java/edu/example/userprofile/resilience/Retry.java
cp /tmp/CircuitBreaker-starter.java src/main/java/edu/example/userprofile/resilience/CircuitBreaker.java
mvn test                # expect: BUILD FAILURE (17 not-passing tests)
```

The same procedure works for grading: drop a student's submission
into `src/main/java/.../resilience/`, run `mvn test`, count
passing tests, read failure messages for diagnostic feedback.

### Lab 3 instructor notes

`labs/lab3-analyzer-in-the-loop/instructor_notes.md` documents
all 8 reliability issues planted in the codebase, with the
analyzer-visibility distribution (3 direct, 2 indirect, 3
invisible), expected student reasoning per issue, and a grading
rubric. Includes a "common student responses to watch for" section
that describes patterns of strong and weak submissions.

## Adoption guidance

Suggested rollout for a 12-15 student SE or data-structures course
across roughly six weeks:

- Week 1: introduce the reliability framework in lecture; assign
  the audit's Section 3.2 paper as background reading.
- Week 2-3: Lab 1 (in-class pair work, ~50 min) + take-home
  worksheet.
- Week 4-5: Lab 2 (in-class lab, ~90 min for implementation, with
  the option to extend into a follow-up session for the
  composition exercise).
- Week 6: Lab 3 (in-class analyzer + prioritization, ~90 min,
  take-home reflection).

For a shorter course, Labs 1 and 2 work as a stand-alone two-week
unit; Lab 3 is the natural extension if time permits.

## Keeping this branch in sync with main

The simplest workflow:

```bash
# When you fix a typo in (say) Lab 1's student README:
git checkout main
# ... edit, commit ...
git push

# Bring those changes over to instructor-materials:
git checkout instructor-materials
git merge main
git push
```

Never merge `instructor-materials` -> `main`. The answer keys
should never appear on the public branch.

## Citation

Same as `README.md` on the `main` branch: cite the paper. After acceptance and
publication, we will replace the anonymized form in `README.md` with the
full BibTeX from `CITATION-POST-PUBLICATION.bib`.

## License

Same as `main`: code under MIT, text under CC BY 4.0. The answer
keys and reference solutions on this branch are also under those
licenses — adopters may modify and redistribute, with attribution.
