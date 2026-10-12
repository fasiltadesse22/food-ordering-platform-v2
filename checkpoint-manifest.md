# Checkpoint Manifest — C1.2-P07 Candidate

Part: 1.2.7 Aggregate Roots, Encapsulation and Invariant Enforcement
Base: `checkpoints/C1.2-P06`, `edde75f6dc63cb4c1568939497ab7cea218c5759`.
Primary evolution: B/C/A — minimally strengthen root invariant, seven regression/negative-control tests, ADR and evidence.
Changes: `OrderInvariants.java`, `OrderInvariantViolationException.java`, `OrderAggregateRootEncapsulationTest.java`, scenario, ADR-0026, experiment/evidence ledgers, this manifest.
Added selected invariant: RESTAURANT_ACCEPTED fact implies current ACCEPTED, PREPARING or COMPLETED under current lifecycle.
Production topology/repository remains exactly one in-process Order authority; no PostgreSQL, transactions, optimistic versioning, Kafka, Saga or service split.
Intentionally preserved: local payment duplicate recording, incomplete full-history semantics, stale-writes P13/P14/P17, JVM-loss P18, external-payment divergence P16.
Required tests: exact-commit Maven root verify and P18 process-restart through GitHub Actions. This authored tree is a **candidate** until CI succeeds.
Handoff: P08 aggregate lifecycle/references and domain services, P09 local facts/events; later persistence and concurrency mechanisms remain deferred.
