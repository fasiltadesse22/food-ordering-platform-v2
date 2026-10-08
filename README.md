# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P07
Current evolution: C1.1-P08 candidate — Terminal, Reversible and Irreversible Outcomes

## Verification

mvn -B -ntp verify

P08 is not frozen until GitHub Actions verifies the exact candidate.

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

## Modification

A real ModifyOrderUseCase now exists.

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
