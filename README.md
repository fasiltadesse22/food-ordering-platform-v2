# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P10
Current evolution: C1.1-P11 candidate — Atomicity and Local Consistency Requirements

## Verification

mvn -B -ntp verify

P11 is not frozen until GitHub Actions verifies the exact candidate.

## Current local atomicity model

Order lifecycle evolution binds:
- next lifecycle state;
- explaining workflow occurrence

inside one OrderEvolution.

One new immutable invariant-checked Order is built before the repository replaces current authority.

## Important non-guarantees

This is NOT:
- a database transaction;
- durability;
- atomic find+modify+save under concurrency;
- multi-Order atomicity;
- distributed atomicity.

## Experiment

P11 preserves a deliberately fragile split status/history model and injects failure after the first write in both orderings.

Both partial states must violate current Order invariants.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory repository;
- no PostgreSQL;
- no @Transactional;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
