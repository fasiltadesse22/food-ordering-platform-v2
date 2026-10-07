# C1.1-P01 — Core Baseline Verification

This experiment is a **local JDK-only fallback verification** for the framework-free core.

It exists because the execution environment used to create this candidate checkpoint had Java 21 but no Maven installation and no network access from the container to retrieve Maven/Spring dependencies.

Run:

```bash
./scripts/verify-core.sh
```

It proves only that the framework-free domain/application/in-memory adapter slice compiles on Java 21 and that the basic place/get flow behaves as asserted.

It does **not** prove:

- the root Maven reactor succeeds;
- Spring Boot starts;
- HTTP tests pass;
- GitHub Actions succeeds;
- the candidate is a valid frozen V8 checkpoint.

The authoritative checkpoint gate remains:

```bash
mvn -B -ntp verify
```

until that command has actually executed successfully.
