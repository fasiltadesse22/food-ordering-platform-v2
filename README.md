# Food Ordering Platform — V8.0 Fresh-Start Baseline

This repository is the evolving project for **Enterprise Distributed Systems Architecture & System Design — V8.0**.

Current verified learning checkpoint:

```text
C1.1-P01
```

Part 1.1.1 intentionally established **one real Spring Boot deployable** without pre-creating microservices or future infrastructure.

## Requirements

- Java 21
- Maven 3.9+
- Spring Boot 4.1.1 (managed by the root POM)

## Authoritative verification

```bash
mvn -B -ntp verify
```

P01 passed this gate in GitHub Actions after an evidence-driven repair to the Spring MVC test classpath.

## Framework-free verification

```bash
./scripts/verify-core.sh
```

This verifies the JDK-only core slice independently of Spring; it complements rather than replaces the Maven checkpoint gate.

## Run

```bash
mvn -pl applications/food-ordering-app spring-boot:run
```

## Architectural guardrails

Not present by design:

- PostgreSQL
- Kafka
- Redis
- Saga
- Outbox
- CQRS
- Event Sourcing
- distributed locks
- service mesh
- Kubernetes
- database-per-service
- premature service decomposition

See `checkpoint-manifest.md` and `architecture/evidence/C1.1-P01-evidence.md` for the exact evidence record.
