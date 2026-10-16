# ADR-0040 — Separate Cross-Authority Planning from Authoritative Local Admission; Do Not Prematurely Distribute

Status: Accepted P21 design subject to final exact-SHA and frozen CI verification.
Inherited C1.2-P20 verified 36284935e29f00a5f4949cda2956bf66e0d72356, V8.0 + derived Cluster1.2 Part1.2.21.

## Problem
Food ordering may depend on separately authoritative stock, Restaurant policy, Payment provider, and Order lifecycle. P20 proved local validity doesn't enforce shared inventory capacity; P19 showed Order write amplification; blindly merging all state into Order or splitting every entity into a service is wrong. Need demonstrate domain service responsibility and reliable local multi-aggregate coordination while not inventing unapproved full Order/Inventory/Payment workflow.

## Decision
1. Implement immutable InventoryAvailabilitySnapshot and InventoryReservationPlan plus pure OrderInventoryReservationPolicy. It aggregates repeated menu-item quantities and checks observed availability and RestaurantId; emits proposal ONLY. This is actual compiled production domain code but not wired into existing PlaceOrder HTTP path, no production Inventory repository, and cannot allocate stock. Do not claim 'reservation confirmed' or derive provider truth from local workflow facts.
2. Implement five test-only real PostgreSQL16 Testcontainers orchestration/failure examples: one shared local tx creates Order+stock admission+reservation; independent Order-first commit leaves Order if stock unavailable; independent stock-first commit consumes capacity before Order; induced reservation write failure rolls back both local authorities inside shared tx; two Orders plan from same availability and only one is admitted by authoritative conditional stock UPDATE.
3. Preserve existing Order production repository/migration unchanged. Test-only p21 tables are not Flyway migrations. Do not wire demo SQL as a user-facing production contract without defining stock allocation lifecycle, cancellation/release, expiry, idempotent operation identity, pricing, and Restaurant/Payment requirements.
4. Keep separate domain, application orchestration, infrastructure authority. No standalone Inventory service or database-per-service decision.

## Alternatives considered
- One giant Order+Restaurant+Inventory+Payment aggregate: false authority, contention; reject.
- One service/database per entity: creates cross-service commit uncertainty without necessity; reject.
- Same PostgreSQL-local multi-aggregate tx (demonstrated): correct for narrowly defined same-DB strong stock-at-placement promise, but cannot include provider settlement.
- Independent sequential commits: observed partial outcomes; valid only if explicit PENDING states, expiration and recovery policy allow.
- SQL SELECT availability then unconditional reservation: TOCTOU, two eligible snapshots oversell; reject.
- Shared conditional inventory stock decrement, reservation and Order INSERT in one local tx (test positive): one admitted, other rolls back.
- Saga, Outbox, CDC, Kafka, Two-Phase Commit: possible later options under justified distribution, not introduced here.

## Tradeoffs and consequences
+ Concrete pure domain service; recognizes duplicate SKU lines and restaurant scope.
+ Real PostgreSQL local atomicity and concurrency experiments, exposing planning-vs-admission distinction.
+ No API regression, no invented provider truth.
- No production stock persistence/Stock aggregate/full Place+Reserve endpoint yet; only test schema and demo coordinator.
- No measured contention/p99; no reservation-release, expiration, idempotency, partial remote processing, retry/reconciliation implementation.
- If a real business rule requires inventory at order placement, implementation must be extended explicitly and all legacy PlaceOrder paths controlled. It is unsafe to claim strong global guarantee while legacy path exists.

## Forward dependency
P22 Cluster1.2 synthesis/exits: identify source of truth, transaction-vs-deployment separation, inventory lifecycle requirements, future Restaurant/Payment context boundaries and delayed learning topics. Freeze only after exact verification.
