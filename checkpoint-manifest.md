# Checkpoint Manifest — C1.2-P14 Final Candidate

Title: Persistence Mapping, Aggregate Reconstitution and Representation Trade-offs.
Roadmap V8.0 Cluster 1.2 / derived P14 blueprint.
Inherited verified checkpoints/C1.2-P13 at 9fd1fe7c3c9a0608c29af5c8397e8bfc3ae9588e.
Implementation+test candidate 8db856eae5d0db83170aad625e66c37b44668aac CI 38695864088 success.
This docs/evidence child commit is candidate until exact-sha CI + frozen-branch CI verified.

Type B production mapping evolution + Type C controlled PostgreSQL tests + Type A ADR/evidence.
Changed PostgresOrderRepository read decoding and added PostgresOrderMapper with persisted list contiguous position checks and exception classification. No schema, business-domain invariant or service boundary change. 7 new real PostgreSQL tests, aggregate and JDBC regression preserved.
Observed candidate CI 175 Maven tests PASS, P18 process-loss negative, P12 durable positive. PostgreSQL BigDecimal 5.5000 scale=4 after retrieval in tested case; Java Instant .123456789 -> .123457, delta211ns. Two collection SQL JOIN 3x3=9 rows.

Open gaps: multiple SELECTs across READ COMMITTED commits may tear, unguarded stale successors, complete collection replacement write amplification, no Money/clock precision policy, no complete historical causality, no external provider truth or separate Restaurant/Payment aggregate tables.
Next P15: Local Transactions, Commit, Rollback and Atomic Persistence. Inherit verified P14 SHA (not main); perform actual injected JDBC/SQL commit/rollback experiments. P16 dedicated isolation work, P17 optimistic concurrency, P18 conflict resolution.
