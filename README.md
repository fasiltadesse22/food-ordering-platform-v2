# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P06
Current evolution: C1.1-P07 candidate — Guards, Preconditions and Postconditions

## Verification

mvn -B -ntp verify

P07 is not frozen until GitHub Actions verifies the exact candidate.

## Current contract layers

Lifecycle action:
1. source-state precondition;
2. contextual domain guard;
3. immutable evolution;
4. explicit postconditions verified by tests;
5. repository current-state replacement only after successful domain evolution.

Examples:
- another restaurant cannot accept this Order;
- another customer cannot cancel this Order;
- refund request requires REJECTED/CANCELLED and a recorded payment.

## Important boundary

Typed acting CustomerId/RestaurantId is business context, not authentication proof.

P07 does not add Spring Security or a trust mechanism for caller identity.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no database;
- no messaging;
- no Saga;
- no service decomposition.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
