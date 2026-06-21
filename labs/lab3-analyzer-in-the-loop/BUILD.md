# Build and Run Notes — Lab 3

## Quick start

```bash
mvn compile           # compile sources
mvn exec:java         # run Main against the in-process API
python3 analyzer_report.py    # run the analyzer
```

## Requirements

- **JDK 17 or later.** The project targets `--release 17`.
- **Maven 3.6 or later.** Check with `mvn --version`.
- **Python 3.8 or later.** The analyzer wrapper uses standard
  library only (no pip install needed).

## The analyzer wrapper

`analyzer_report.py` expects the Section 3.2 audit runner at
`../../audit/audit_runner.py` relative to the Lab 3 directory.
That is the default location in the replication repository:

```
AI RELIABILITY/
├── audit/
│   └── audit_runner.py          ← the analyzer
└── labs/
    └── lab3-analyzer-in-the-loop/
        ├── analyzer_report.py   ← this wrapper
        └── ...
```

If the audit runner is elsewhere, pass its path:

```bash
python3 analyzer_report.py --audit-runner /path/to/audit_runner.py
```

To save the raw CSV produced by the analyzer (useful if you want
to diff before/after directly):

```bash
python3 analyzer_report.py --csv before.csv
# ... apply a fix ...
python3 analyzer_report.py --csv after.csv
diff before.csv after.csv
```

## Common errors

**`audit_runner.py not found`**
The wrapper couldn't find the audit script at its default location.
Pass `--audit-runner` with the correct path.

**`No .java files found under src/main/java`**
You're running the wrapper from outside the Lab 3 directory, or
the source tree is missing. `cd` into the Lab 3 root.

**`Address already in use`**
The demo's in-process server picks an OS-assigned port (port 0),
so this should not happen. If it does, wait a few seconds and
retry.

**`ClassNotFoundException: com.fasterxml.jackson...`**
You ran `java` directly instead of `mvn exec:java`. Use Maven so
the classpath includes Jackson.

## IDE setup

Prefer: VScode, but both IntelliJ IDEA and Eclipse can import this project directly
from `pom.xml`. In IntelliJ: File → Open → select the project
root. In Eclipse: File → Import → Maven → Existing Maven Projects.
