# Build and Run Notes

## Quick start

```bash
mvn compile           # compile sources
mvn exec:java         # run Main against the in-process API
```

Expected runtime: a few seconds.

## Requirements

- **JDK 17 or later.** The project targets `--release 17`. Newer JDKs
  (e.g., 21, 23) work fine. Older JDKs do not — `URI.create(...).toURL()`
  was added in 20+, and the `Map.of(...)` factory methods used in `Main`
  require 9+.

  Check your version:

  ```bash
  java --version
  ```

- **Maven 3.6 or later.** Check with `mvn --version`.

## First-time Maven setup

Maven will download Jackson on the first build. The download is small
(a few MB) and only happens once. If your build fails with a network
error on first compile, the cause is almost always a corporate firewall
or proxy. Configure Maven via `~/.m2/settings.xml` or run from a
network with `repo.maven.apache.org` reachable.

## Running just the compilation

```bash
mvn compile
```

This is enough to verify the code builds. If you want to package a JAR
(not needed for Lab 1):

```bash
mvn package
```

## Common errors

**`ClassNotFoundException: com.fasterxml.jackson...`**
You ran the compiled `.class` files directly via `java` instead of via
`mvn exec:java`. Maven adds Jackson to the classpath automatically;
plain `java` does not. Use `mvn exec:java`.

**`Address already in use`**
The fake HTTP server picks an OS-assigned port (it asks for port 0),
so this should not happen. If it does, you have a previous run still
holding a socket — wait a few seconds and retry, or restart your shell.

**`error: invalid target release: 17`**
Your JDK is older than 17. Install a newer JDK (21 LTS is a good
choice).

## IDE setup

Both IntelliJ IDEA and Eclipse can import this project directly from
`pom.xml`. In IntelliJ: File → Open → select the project root. In
Eclipse: File → Import → Maven → Existing Maven Projects.
