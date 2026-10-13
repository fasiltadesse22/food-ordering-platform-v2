# C1.2-P11 — Real PostgreSQL/Flyway/Testcontainers Experiments

## Environment and authority
Real PostgreSQL 16 container via Testcontainers (Docker), PostgreSQL JDBC, Flyway V1, root Maven 21 on GitHub Actions; NOT a fake H2/in-memory SQL emulator. P11 Order runtime remains in-memory.

## Experiments
E1 Migration repeatability: Flyway V1 runs once; subsequent migrate reports zero new migrations. Observation: PASS in candidate CI run 38356670113.
E2 Actual schema: information_schema contains orders, order_lines, order_workflow_occurrences. Observation PASS.
E3 Database PK identity uniqueness: insert same UUID twice -> SQLState 23505, first row remains. Observation PASS. Not application createNew adapter.
E4 Child referential integrity: orphan line and occurrence -> SQLState 23503. Observation PASS. No Customer/Restaurant cross-domain existence is inferred.
E5 Explicit list order: out-of-order inserts; ORDER BY position reconstructs ordered rows; duplicate position -> 23505. Observation PASS. Does not enforce contiguous positions.
E6 Monetary precision: JDBC setBigDecimal and NUMERIC read back high-scale value without two-place rounding. Observation PASS. No currency or arbitrary extreme magnitude guarantee.
E7 Temporal precision: PostgreSQL TIMESTAMPTZ read to OffsetDateTime and Instant exactly for microsecond-precision test value. Observation PASS. Nanosecond values may be rounded/truncated and must be handled by later mapping contract.
E8 Intentionally incomplete domain constraint: direct SQL inserts zero quantity/negative price and reads it back successfully. Observation PASS as a negative control; P13 must evaluate defense-in-depth constraints.

## Experiment template
Each: question -> hypothesis -> prediction -> setup -> controlled variable -> execution -> observation -> evidence -> interpretation -> limitations -> conclusion. Source test: OrderRelationalSchemaMigrationTest.java. First run 38356670113 verified; final evidence-bearing SHA must be separately verified. Inherited P18 real SIGKILL -> 404 negative-control runs during CI to show that P11 has not yet changed runtime persistence.

## Limitations
No JDBC Order adapter, no multi-table atomic commits, no ORM mapping, no full Order history reconstruction, no optimistic version/cas, no isolated concurrent transaction stress, no restart recovery, no query latency benchmarking. These requirements are scheduled later.
