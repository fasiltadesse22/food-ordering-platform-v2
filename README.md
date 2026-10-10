# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P12
Current evolution: C1.1-P13 candidate — Conflicting Operations and Concurrency Windows

## Part type

Type C — controlled concurrency failure evolution.

## Verification

mvn -B -ntp verify

P13 is not frozen until the exact candidate executes successfully in CI.

## Current concurrency shape

OrderWorkflowService:

find current Order
→ make decision against returned immutable snapshot
→ save new immutable successor

InMemoryOrderRepository:

ConcurrentHashMap get/put

There is no version comparison across the read-decide-write sequence.

## P13 experiment

ACCEPT and REJECT are both valid from PLACED.

The deterministic test forces both operations to capture the same PLACED snapshot, then controls save ordering.

Expected vulnerable behavior:
- both callers can receive Accepted;
- last save becomes current authority;
- reversing save order reverses final authority.

## Important scope

P13 intentionally does not fix the race.

It preserves the fragile state for later comparison.

P14 will apply the same concurrency mechanism to the required customer-cancellation versus restaurant-acceptance race.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no locking/versioning;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
