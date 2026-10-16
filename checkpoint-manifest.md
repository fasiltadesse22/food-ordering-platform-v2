# Checkpoint Manifest — C1.2-P21 Candidate (verify final SHA and frozen CI)

Scope Part 1.2.21 Cross-Aggregate Operations, Domain Services and Future Distribution Pressure per Master Roadmap V8.0 and Cluster1.2 derived blueprint. Parent frozen verified C1.2-P20 36284935e29f00a5f4949cda2956bf66e0d72356. One Food Ordering Platform monorepo, Java21 + PostgreSQL16.

Type B production domain-only evolution: four new domain/inventory files (InventoryAvailabilitySnapshot, InventoryReservationPlan, InventoryReservationPlanningException, OrderInventoryReservationPolicy). This computes immutable proposal only; NOT authoritative stock allocation, not automatically wired to PlaceOrder HTTP or production Inventory persistence.
Type C experiments: six pure policy tests plus five real PostgreSQL tests covering atomic local multi-aggregate order+stock reservation, Order-first partial, stock-first partial, late SQL failure rollback, two racing proposals with shared conditional admission. p21_* tables and JDBC coordinator exist ONLY under test source; no Flyway V4 production schema.
Type A docs: scenarios, ADR-0040, experiments, evidence, runbook and manifest.
No production API changes, no distributed deployment, broker, provider charges/refunds, fully defined inventory lifecycle, Restaurant intake model, automatic compensation/retry, or false stock-at-placement guarantee.
Required gate: root Maven verify and P18 memory restart negative/P12 PostgreSQL actual process restart positive must pass on exact documentation-bearing SHA; create frozen checkpoints/C1.2-P21 only after work CI success, verify frozen branch run exact same SHA. Next P22 cluster 1.2 synthesis/production readiness/exit assessment; do not preempt cluster1.3.
