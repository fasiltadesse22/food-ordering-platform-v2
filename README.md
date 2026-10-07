# Food Ordering Platform — V8.0 Fresh-Start Evolution

This repository is the evolving project for **Enterprise Distributed Systems Architecture & System Design — V8.0**.

Inherited verified checkpoint:

```text
C1.1-P01
branch: checkpoints/C1.1-P01
```

Current evolution:

```text
C1.1-P02 candidate
Actors, Goals, Responsibilities and Use Cases
```

## Verification

```bash
mvn -B -ntp verify
```

P02 is not frozen until its GitHub Actions verification completes successfully.

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

See `checkpoint-manifest.md` and `architecture/evidence/` for evidence-qualified claims.
