# Food Ordering Platform — V8.0 Fresh-Start Evolution

This repository is the evolving project for **Enterprise Distributed Systems Architecture & System Design — V8.0**.

Current verified learning checkpoint:

```text
C1.1-P02
Actors, Goals, Responsibilities and Use Cases
```

Previous preserved checkpoint:

```text
checkpoints/C1.1-P01
```

## Verification

```bash
mvn -B -ntp verify
```

P02 verification executed in GitHub Actions:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Current topology

- one Java 21 / Spring Boot deployable;
- framework-independent domain/application core;
- in-memory persistence adapter;
- minimal HTTP adapter;
- explicit application-level business actor context for Place Order.

## Important actor-model boundary

`ActorContext` expresses **business use-case responsibility**. It is not authentication and does not prove the caller's real-world identity.

## Deliberately absent

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

See `checkpoint-manifest.md`, `architecture/scenarios/`, and `architecture/evidence/` for the scope and evidence record.
