# Checkpoint Manifest — C1.1-P07

## Identity

- Checkpoint: C1.1-P07
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P06
- Inherited branch: checkpoints/C1.1-P06
- Inherited commit: a0a11c303b11554545e9dac08d5f64661b1e217f
- P07 implementation commit: e20d29a87a5a969a40f9ed8c1f8782b3877f9581
- P07 verification run: 37753011313

## Engineering question

Even when a transition is structurally legal from the current state, what additional facts must be true before it is allowed, and what must be true afterward?

## Verified preconditions and guards

Restaurant-side lifecycle actions:
- P06 source-state legality;
- acting RestaurantId must equal Order.restaurantId.

Cancellation:
- P06 source-state legality;
- acting CustomerId must equal Order.customerId.

Refund request:
- lifecycle must be REJECTED or CANCELLED;
- PAYMENT_RECORDED must exist.

## Verified postconditions

For successful acceptance:
- OrderId preserved;
- CustomerId preserved;
- RestaurantId preserved;
- lines preserved;
- placedAt preserved;
- total preserved;
- status becomes ACCEPTED;
- exactly one RESTAURANT_ACCEPTED occurrence is appended;
- original immutable Order remains unchanged.

For failed contextual guard through the application service:
- repository current state is not replaced;
- attempted occurrence is absent.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Architectural interpretation

P07 adds domain guards and transition contracts.

It does not add authentication/security infrastructure.

Typed acting CustomerId/RestaurantId is claimed/trusted application context, not proof of caller identity.

The project still contains no:
- Spring Security lifecycle authorization flow;
- PostgreSQL;
- Kafka;
- Redis;
- Saga;
- Outbox;
- optimistic locking;
- distributed lock;
- service split.

## Evidence-qualified guarantees

Within the tested one-process model:
- selected contextual ownership guards reject mismatched identities;
- selected refund preconditions reject invalid requests;
- successful transition postconditions hold;
- failed guards prevent authoritative-state replacement.

Not guaranteed:
- identity authenticity;
- stale-state correctness;
- concurrency safety;
- duplicate safety;
- durability;
- external refund behavior.

## Next pressure

Part 1.1.8 must classify terminal, reversible and irreversible lifecycle outcomes and distinguish reversal from compensation.
