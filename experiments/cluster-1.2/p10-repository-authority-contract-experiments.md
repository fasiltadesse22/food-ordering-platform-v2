# C1.2-P10 — Repository Authority Contract Experiments

Inheritance: verified checkpoints/C1.2-P09, 000c2960fdbbe85106fc68c051193f5fafc37675.
**Controlled tests:** OrderRepositoryAuthorityContractTest (9 tests).

1. Initial create: absent OrderId -> createNew -> findCurrentById returns the exact Order instance (in-process only).
2. Collision: same OrderId, different CustomerId -> typed OrderAlreadyExistsException; original representation stays unchanged.
3. Parallel creation: 32 contenders in thread pool, start latch; exactly one success, 31 conflicts, one map entry. This validates ConcurrentHashMap putIfAbsent for a SINGLE authority instance, not distributed uniqueness.
4. Normal update: successful Order.recordRestaurantAcceptance + saveCurrent publishes successor; original immutable state unchanged.
5. Stale update negative: initial PLACED -> ACCEPTED current -> stale CANCELLED successor overwrites; this is expected P17 fragility and must remain.
6. SaveCurrent on missing key negative: still creates entry; future update-only/versioned contract needed; test is not evidence of correctness.
7. PlaceOrderService path: createNew count increments, legacy saveCurrent count zero, OrderPlaced returned only after successful creation.
8. Typed repository collision from controlled adapter prevents accepted placement result and fallback overwrite.
9. New repository instance lacks Order from old instance; contrast with real P18 SIGKILL experiment.

Methods: Question -> hypothesis -> prediction -> setup -> controlled variable -> execution -> observation -> evidence -> interpretation -> limitations -> conclusion. Report success only after exact-commit CI root Maven + process-restart script. P13/P14/P16/P17 continue as inherited negative controls.
