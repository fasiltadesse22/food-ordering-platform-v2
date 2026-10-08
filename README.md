# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P05
Current evolution: C1.1-P06 candidate — State Machines and Legal/Illegal Transitions

## Verification

mvn -B -ntp verify

P06 is not frozen until GitHub Actions verifies the exact candidate.

## Current Order lifecycle

PLACED
  → ACCEPTED → PREPARING → COMPLETED
  → REJECTED
  → CANCELLED

Payment/refund remain workflow milestones rather than OrderStatus values.

## Important boundary

P06 enforces source-state legality only.

It does not yet fully define:
- contextual guards/preconditions;
- terminality;
- reversibility/compensation;
- concurrency/stale-state behavior;
- repeated-command semantics.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no state-machine framework;
- no database;
- no messaging;
- no Saga;
- no service decomposition.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
