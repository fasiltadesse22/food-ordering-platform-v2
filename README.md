# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P07 — Guards, Preconditions and Postconditions
Previous checkpoint: checkpoints/C1.1-P06

## Verification

mvn -B -ntp verify

P07 implementation verification:

Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Current contract layers

Lifecycle action:
1. source-state precondition;
2. contextual domain guard;
3. immutable evolution;
4. explicit postconditions;
5. repository replacement only after successful domain evolution.

Verified examples:
- another restaurant cannot accept this Order;
- another customer cannot cancel this Order;
- refund request requires REJECTED/CANCELLED plus PAYMENT_RECORDED;
- guard failure leaves authoritative repository state unchanged.

## Important boundary

Typed acting CustomerId/RestaurantId is business context, not authentication proof.

P07 does not add Spring Security or establish a trust mechanism for caller identity.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no database;
- no messaging;
- no Saga;
- no service decomposition.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
