# Checkpoint Manifest — C1.2-P20 candidate until final exact SHA and frozen CI verified

Scope: Part 1.2.20 Undersized Aggregates and Cross-Aggregate Invariants; Master Roadmap V8.0 + derived Cluster1.2 blueprint. Inherited authoritative frozen C1.2-P19 3c1d8b56fd2cb1b1f30b2fb95ae2e0a5c2b7e8ae; never use main for baseline.

Chosen Type C evidence evolution + Type A ADR/scenario/runbook. No production aggregate split, new Flyway migration, external Payment adapter or Inventory authority. New real PostgreSQL test suite PostgresOrderUndersizedBoundaryTest six cases: (1) status-first wrong split (2) history-first wrong split (3) later independent failure does not undo earlier commit (4) one local transaction reader observes old or new valid state (5) hypothetical test-only independent reservations oversell shared capacity (6) shared test-only conditional admission prevents oversell under concurrent clients.

Required exact CI: root Maven reactor/6 tests all pass; inherited P18 memory SIGKILL negative and P12 PostgreSQL recovery positive. Freeze only exact docs-bearing successful commit SHA; verify frozen-branch CI. P21 next true independent authorities and coordination, P22 cluster synthesis. Source claims and limitations in architecture/evidence/C1.2-P20-evidence.md.
