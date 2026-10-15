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

## Initial executed measurement evidence
At code/test SHA 971f2e176d74467d3f1152fa19d65964c3e4d8a0, CI run https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38777573425 SUCCESS: root 217 tests, new P19 5 tests, both inherited SIGKILL scripts passed. Real DB audit: L12/H24 one milestone => 12+12+24+25=73 child row operations. Same Order one winner/one conflict; different Order identities two winners/no conflict. Samples and limitations in architecture/evidence/C1.2-P19-evidence.md. Final docs-bearing SHA/frozen branch run must still pass before freeze.
