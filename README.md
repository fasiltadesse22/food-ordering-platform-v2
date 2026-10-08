# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P05 — End-to-End Workflow Discovery
Previous checkpoint: checkpoints/C1.1-P04

## Verification

mvn -B -ntp verify

P05 implementation verification:

Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Current workflow model

The project records selected in-process workflow milestones for:
- payment;
- restaurant acceptance/rejection;
- cancellation;
- refund request;
- preparation;
- completion.

The recorder is intentionally permissive.

Order.status remains PLACED because P05 is workflow discovery, not the lifecycle state machine.

## Important boundaries

- PAYMENT_RECORDED is a local learning-stage milestone, not a real external charge.
- REFUND_REQUESTED is not REFUND_COMPLETED.
- workflow occurrence list is not Event Sourcing.
- participant handoffs do not imply service/network boundaries.
- contradictory sequences currently remaining executable is evidence of missing lifecycle guards.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no database;
- no messaging;
- no Saga;
- no service decomposition.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
