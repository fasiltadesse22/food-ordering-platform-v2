# Checkpoint Manifest — C1.1-P07 Candidate

## Identity

- Target checkpoint: C1.1-P07
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P06
- Inherited branch: checkpoints/C1.1-P06
- Inherited commit: a0a11c303b11554545e9dac08d5f64661b1e217f

## Engineering question

Even when a transition is structurally legal from the current state, what additional facts must be true before it is allowed, and what must be true afterward?

## Preconditions and guards introduced

Restaurant-side operations:
- source-state legality from P06;
- acting RestaurantId must equal Order.restaurantId.

Cancellation:
- source-state legality from P06;
- acting CustomerId must equal Order.customerId.

Refund request:
- Order must be REJECTED or CANCELLED;
- PAYMENT_RECORDED must exist.

## Postconditions made explicit

For successful lifecycle transitions:
- Order identity is preserved;
- customer/restaurant ownership is preserved;
- stable order content is preserved;
- expected target state is reached;
- exactly one expected workflow occurrence is appended;
- original immutable representation is unchanged.

On failed state/guard checks:
- no new current Order is saved;
- attempted occurrence is not appended.

## Architectural interpretation

P07 introduces domain guards and contracts.

It does not introduce authentication or security infrastructure.

The supplied acting CustomerId/RestaurantId is claimed business context, not proof of caller identity.

The project still contains no:
- Spring Security authorization flow for these lifecycle actions;
- PostgreSQL;
- Kafka;
- Redis;
- Saga;
- Outbox;
- optimistic locking;
- distributed lock;
- service split.

## Evidence status

Pending P07 CI.

## Fragilities intentionally preserved

- business identity can be forged if an untrusted adapter supplies it;
- guards can evaluate stale Order state;
- no concurrency protection;
- no repeated-command semantics;
- current state is transient.

## Next pressure

Part 1.1.8 must interpret terminal, reversible and irreversible lifecycle outcomes and distinguish reversal from compensation.

## Freeze gate

1. mvn -B -ntp verify succeeds;
2. inherited tests remain green where semantically applicable;
3. wrong restaurant/customer are rejected;
4. refund preconditions are enforced;
5. successful transition postconditions are observed;
6. guard failure leaves repository authority unchanged.
