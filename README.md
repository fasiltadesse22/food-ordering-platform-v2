# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P08 — Terminal, Reversible and Irreversible Outcomes
Previous checkpoint: checkpoints/C1.1-P07

## Verification

mvn -B -ntp verify

P08 implementation verification:

Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Current lifecycle semantics

Terminal Order states:
- REJECTED
- CANCELLED
- COMPLETED

Terminality concerns OrderStatus transitions. It does not prevent later compensating workflow actions.

## Compensation

PAYMENT_RECORDED
→ cancellation/rejection
→ REFUND_REQUESTED

Refund request is a new action. It does not erase payment history and does not reopen the terminal Order.

A second semantic refund request is rejected. Idempotent retry semantics are still deferred.

## Modification

A real ModifyOrderUseCase exists.

Modification is allowed only while PLACED and by the owning customer.

Important: payment currently leaves the Order PLACED, so modification-after-payment remains an intentionally preserved later fragility.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no Saga;
- no distributed compensation;
- no database;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
