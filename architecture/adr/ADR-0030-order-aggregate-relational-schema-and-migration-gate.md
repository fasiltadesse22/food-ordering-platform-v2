# ADR-0030 — Represent One Order Aggregate in Three PostgreSQL Tables; Migrate and Test Before Runtime Cutover

Status: Accepted for C1.2-P11.
Inherited verified C1.2-P10 (716a48ebf4b1591c2dbcc6aa014f1ca335b9769b).

## Context
Order is one immutable aggregate with OrderId UUID, CustomerId, RestaurantId, lines, OrderStatus, placement Instant, and ordered workflow occurrences. P03 required state/history atomicity. P10 differentiated create-new identity uniqueness from unsafe successor saves. P18 showed in-memory crash-loss. V8.0 requires real PostgreSQL, migrations, reproducible Testcontainers and later transactions/concurrency experiments.

## Decision
Add a versioned Flyway V1 PostgreSQL migration defining orders root plus order_lines and order_workflow_occurrences children, with explicit position columns and FK/PK. Use NUMERIC without fixed scale (P06 monetary semantic not yet decided) and TIMESTAMPTZ for Instant with explicit microsecond caveat. Do not persist a derived total. Keep CustomerId/RestaurantId scalar references; do not invent Customer/Restaurant/Payment tables.
Add test-scoped Flyway, PostgreSQL JDBC, Testcontainers; run real PostgreSQL schema tests in root CI without changing in-memory runtime wiring. Testcontainers code is intentionally isolated to tests; main migration resource is production artifact, not a mock.

## Alternatives
1. Put everything in one JSONB root row: simple whole-object storage, but obscures relational child integrity/ordering and reduces opportunities for later relational-transaction experiments; not chosen.
2. Store lines/history in three tables with child position keys: chosen, expresses observed domain lists.
3. One table per supposed microservice: rejects table -> aggregate -> service conflation.
4. Runtime PostgreSQL adapter and @Transactional in P11: skips P12 durable-recovery and P15 transaction-stage gates; deferred.
5. NUMERIC(12,2) + Money now: imposes unapproved precision and currency assumptions; reject.
6. Store Order total as independent column: duplicate mutable state requires total/line invariant; reject until domain authorizes distinct agreed amount.

## Non-guarantees
V1 PK+FK constrain structural relationships but NOT all OrderLine/business invariants; negative test stores quantity=0/negative price intentionally. Schema has no business state-machine/chronology/actor validity proof, no multi-row commit API, no optimistic version, no provider charge, no event store.
A CI schema test is NOT a durable running Order repository; inherited P18 restart-loss must continue until P12.

## Migration rules
Never edit already applied V1 in deployed DB. Future changes use new migrations; test Flyway checksum/version history and rolling-deploy compatibility when required.
PostgreSQL NUMERIC may have trade-offs in storage/CPU; benchmark later rather than invent performance claims. TIMESTAMPTZ stores instants with microsecond precision versus Java nanoseconds.

## Verification
Initial schema+test candidate 62ecbf4f5694d06d1aed0b5d678c6430253e33a2 passed CI 38356670113, root Maven tests and P18 script. Freeze only after final docs/evidence commit passes the same tests on its own exact SHA.
