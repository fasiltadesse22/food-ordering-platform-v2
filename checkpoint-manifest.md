# Checkpoint Manifest — C1.2-P16 Final Candidate

Part 1.2.16 Database Isolation and Concurrent Aggregate Modification.
Source contract V8.0 roadmap and derived Cluster1.2 blueprint, 20 analytical obligations and verified project continuity.
Inherited C1.2-P15 SHA aae54a4346f190cc02ff809b241c6fc64c11d3b8.
Implementation/test SHA 1e12bfb1b94245d26fb2cd347b7a852104044c57, GitHub CI run 38752561425 SUCCESS; 194 root Maven tests, nine new P16, 0 failures/errors/skipped, P18 original negative and P12 Postgres durability positive.
Docs-bearing final commit is only candidate until its own CI and frozen-branch exact SHA CI both pass.

Production evolution: PostgresOrderRepository.findCurrentById now bounded read-only REPEATABLE READ transaction across three SELECTs, commit/rollback; package-private RootReadObserver NOOP production deterministic P16 interleaving seam. No schema changes, no optimistic version CAS, no extra service, no external payment or new Customer/Restaurant authority.
Verification: direct SQL READ COMMITTED torn snapshot and nonrepeatable read, repeatable-read coherent, real repo paused between SELECTs while writer commits, ordinary MVCC read vs FOR UPDATE NOWAIT, lock timeout, two conflicting concurrent immutable successors both accepted under unversioned saveCurrent, repeatable-read raw UPDATE SQLState 40001, no-row behavior.
Intentional fragile next-state: concurrent acceptance and rejection both acknowledged; one last-writer-wins, no conflict-specific error; read snapshot coherent but potentially stale immediately after loading. P17 must implement optimistic version/currentness enforcement with source version and DB conditional update; P18 retry reconciliation/idempotency. Do not modify main; next part inherits exact verified P16 frozen SHA.
