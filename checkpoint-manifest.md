# Checkpoint Manifest — C1.2-P15 Final Candidate

Part: 1.2.15 Local Transactions, Commit, Rollback and Atomic Persistence.
Authority: Master Roadmap V8.0 and derived Cluster 1.2 blueprint; one continuous Food Ordering Platform repository. Inherited verified checkpoints/C1.2-P14 SHA 9143f1f627423ad8da4cef1a41c8ae20d0045775. Implementation candidate 4ac8353f0a36dcfe7039d0a24da6da499c4e14e7 CI 38697855420 SUCCESS.

Source evolution: PostgresOrderRepository explicit local transaction retained; tracks COMMIT attempt and preserves not-known-rollback semantics; package-private ConnectionSource for test-specific lost ACK after genuine commit; OrderPersistenceException extensible; new OrderCommitOutcomeUnknownException with OrderId; no schema/domain model change, no Spring service split.
New PostgreSQL 16 Testcontainers class PostgresOrderTransactionBoundaryTest: 10 controlled experiments with actual DB triggers, deferred trigger, actual uncommitted server session termination, separate connection read, and simulated postcommit ack loss. Production SQL migrations untouched; temporary failure triggers only in isolated test DB.
Expected and observed: after failed partial DML full old aggregate persists; after failed creation none persists; deferred commit error aborts DB transaction; after successful COMMIT complete aggregate persists; ack loss client error can coexist with committed aggregate.
Candidate root Maven 185 tests pass (0 failures/errors/skipped), original memory P18 404, PostgreSQL P12 fresh JVM 200 CANCELLED recovery pass.

Deferred known risks: READ COMMITTED separate read-query tearing P16; P17 stale accepted/cancel business conflict overwrites; P18 retry/reconciliation/command idempotency; postcommit network ambiguity not globally resolved; provider Payment authority not under this local DB transaction; no distributed transactions. No throughput/locking/HA benchmark.
Exact docs-bearing final commit and frozen branch require independent CI verification before marking frozen.
Next Part 1.2.16 Database Isolation and Concurrent Aggregate Modification; inherit frozen P15 SHA not main.
