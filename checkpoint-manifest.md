# Checkpoint Manifest — C1.2-P19 candidate

Scope: Part 1.2.19 Aggregate Sizing, Contention and Transaction Cost.
Authoritative: Master Roadmap V8.0 + derived Cluster1.2 P1.2.19.
Parent actual verified/frozen checkpoint C1.2-P18, SHA 752e1def3b66029d659d6344721158b40f2cf6c5. Do NOT use main.
Chosen evolution: Type C real PostgreSQL experimental suite + Type A architecture/evidence design; NO unproven production aggregate split or SQL rewrite.
New executable suite: PostgresOrderAggregateSizingTest, E1 workload matrix observed per-operation nanoseconds with correctness assertions; E2 test-only trigger count of actual row deletions/insertions; E3 same-Order root CAS hot spot for disjoint concerns; E4 distinct Order version independence; E5 stale larger replacement preserves complete winner.
Documentation: P19 scenario, ADR-0038, experiment methodology, evidence. Structural equations: reads 1+L+H rows across three SELECTs; one milestone current writer child operations 2L+2H+1; sample profiles (1,0),(8,16),(48,96); trigger L12,H24 =>73 child operations.
Checkpoint gate: root Maven/cluster relevant tests and both inherited P18 memory-loss negative and P12 PostgreSQL JVM recovery must succeed at exact docs-bearing SHA, then create checkpoint branch same SHA and verify its independent CI. Pending.
Nonclaims: no production p99, no measured optimal aggregate size, no quantified WAL, no automatic split, no external provider correctness.
Forward Part 1.2.20 undersized aggregate/cross-aggregate invariants; Part 1.2.21 coordination; P22 cluster synthesis.
