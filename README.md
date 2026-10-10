# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P16
Current evolution: C1.1-P17 candidate — Stale-State Decisions & Temporal Correctness

## Part type

Type C — controlled stale-state / temporal-correctness failure evolution.

## Verification

mvn -B -ntp verify

P17 is not frozen until the exact candidate passes CI.

## P17 question

A state can be authoritative when observed and stale later.

What happens if a decision based on the earlier state is applied after authority changes?

## Controlled evidence

The new stale-state harness studies:
- modification after an intervening payment;
- payment after an intervening cancellation;
- fresh payment-after-cancellation as a separate policy finding;
- a modification guard that correctly rejects fresh ACCEPTED state but passes on a stale PLACED copy.

## Critical distinctions

stale data != stale decision.

correct guard != current-state guarantee.

immutability != freshness.

missing business policy != stale-write corruption.

## Important boundary

No versioning, locking or PostgreSQL mechanism is added in P17.

P18 next studies process crash/transient-state/durability non-guarantees.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no version field;
- no expected-version save;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
