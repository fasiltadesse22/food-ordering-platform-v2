# Checkpoint Manifest — C1.2-P11 Final Candidate

Title: Relational Persistence Design and Schema Evolution.
Roadmap: Course 2 V8.0, Cluster 1.2, derived blueprint 1.2.11.
Inherited verified C1.2-P10 at 716a48ebf4b1591c2dbcc6aa014f1ca335b9769b.
Implementation candidate: 62ecbf4f5694d06d1aed0b5d678c6430253e33a2 verified CI 38356670113.
Final commit: this documentation/evidence child commit is a candidate until its own exact CI verifies.

Project evolution Type B/C/A:
- Production SQL: V1__create_order_aggregate_schema.sql, one Order represented across three related tables.
- Test-scoped PostgreSQL JDBC, Flyway, PostgreSQL Testcontainers.
- 8 SQL integration tests with real PostgreSQL database (including P13 negative-control).
- ADR-0030, scenario, experiment/evidence documentation, manifest.
- Runtime Spring app wiring remains InMemoryOrderRepository. No durable application behavior yet.

Critical guarantees:
PostgreSQL UUID PK rejects duplicate Order identity, child FK rejects orphan, per-Order child position PK preserves unique slot, Flyway V1 once and repeat migration no-op.
Non-guarantees:
No domain model reconstruction from DB yet, no real app PostgreSQL reads/writes, no atomic multi-table aggregate save, no price/quantity full integrity CHECK, no currency/rounding policy, no optimistic version, no Kafka/outbox, no external payment integration.
Preserve P18 real process-loss failure through CI.
P12 MUST implement actual runtime PostgreSQL repository, controlled reconstitution, migrations on startup/managed lifecycle, and repeat real P18 process replacement against same DB to demonstrate recovery.
