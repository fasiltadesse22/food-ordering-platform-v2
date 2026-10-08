# Checkpoint Manifest — C1.1-P08

## Identity

- Checkpoint: C1.1-P08
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P07
- Inherited branch: checkpoints/C1.1-P07
- Inherited commit: 6f4ca2c2d073575f6eaa831e0031d6f7205e124e
- P08 implementation commit: 1deeb85e6e85ab4a5e848786d1cbc9c291fc77ba
- P08 verification run: 37783525459

## Engineering question

Which lifecycle outcomes can truly be undone, which cannot, and when must the business perform a new compensating action instead of pretending the original fact never occurred?

## Verified terminal classification

Terminal:
- REJECTED
- CANCELLED
- COMPLETED

Nonterminal:
- PLACED
- ACCEPTED
- PREPARING

## Verified compensation semantics

- PAYMENT_RECORDED is retained as historical truth;
- REFUND_REQUESTED is a later compensating workflow occurrence;
- eligible refund request does not change terminal OrderStatus;
- second semantic refund request is rejected.

## Verified modification semantics

- real ModifyOrderUseCase exists;
- owning customer can modify while PLACED;
- modification keeps lifecycle PLACED and appends ORDER_MODIFIED;
- terminal Orders cannot be modified;
- failed terminal modification leaves repository authority unchanged.

## Important preserved fragility

A paid Order remains PLACED and can therefore still be modified.

P08 intentionally does not solve modification-after-payment.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Architectural interpretation

P08 models local lifecycle terminality and compensation semantics.

It does not introduce:
- Saga;
- rollback coordinator;
- external refund provider;
- Event Sourcing;
- PostgreSQL;
- Kafka;
- Redis;
- optimistic locking;
- distributed lock.

## Evidence-qualified guarantees

Within the tested one-process model:
- selected terminal states are explicit;
- completed/cancelled terminal restrictions are enforced;
- compensation history is additive;
- first refund request is preserved when a second is rejected;
- PLACED modification works.

Not guaranteed:
- external financial effects;
- distributed compensation;
- retry idempotency;
- durability;
- concurrency/stale-state correctness.

## Next pressure

Part 1.1.9 must distinguish business failure from technical failure and evolve failure semantics without collapsing expected business rejection into infrastructure error.
