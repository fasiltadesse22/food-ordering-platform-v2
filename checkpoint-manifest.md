# Checkpoint Manifest — C1.1-P04

## Identity

- Checkpoint: C1.1-P04
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P03
- Inherited branch: checkpoints/C1.1-P03
- Inherited commit: 5806426484a646a27df26955d56021e29a1a9f14
- P04 implementation commit: d6d6d31b74e3fed7a3620983cef14d1650e339e7
- P04 verification run: 37639145313

## Engineering question

How do we know which business thing we are talking about, and which representation of its state should be trusted?

## Prediction

Typed business identifiers plus an explicit current-state repository contract can distinguish identity from object reference and snapshots from authority while preserving the single-process in-memory architecture.

## Source evolution

- OrderRepository exposes saveCurrent/findCurrentById;
- GetOrderUseCase accepts OrderId rather than transport String;
- GetOrderService queries current state by typed identity;
- OrderSnapshot preserves typed identities;
- OrderController parses HTTP path String into OrderId;
- OrderResponse converts application/domain identity values to transport strings.

## Experiment evolution

OrderIdentityAndAuthorityTest verifies:
- equal identifier values across different OrderId objects;
- same business identity across different Order Java instances;
- repository selection of current same-identity representation;
- detached snapshot becoming stale;
- unknown identity lookup.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

All inherited P01-P03 tests remained green.

## Architectural interpretation

P04 defines current authority, not database architecture.

It introduces no:
- PostgreSQL;
- JPA/Hibernate;
- database primary-key decision;
- optimistic lock/version field;
- distributed lock;
- cache;
- replica;
- service split.

## Evidence-qualified guarantees

Within the tested one-process model:
- equal OrderId values address the same repository identity;
- different Java objects can represent the same Order identity;
- repository mapping determines current in-process Order state;
- detached snapshots do not automatically track later current state;
- unknown OrderId has no current Order.

Not guaranteed:
- durability;
- stale-write rejection;
- concurrent update correctness;
- database uniqueness;
- distributed source-of-truth coordination.

## Non-guarantees deliberately preserved

- process restart loses authority state;
- repository overwrite is concurrency-naive;
- lifecycle-valid update rules are not defined;
- no version field exists.

## Next pressure

Part 1.1.5 moves into end-to-end workflow discovery: ordering, payment, restaurant decision, cancellation, refund, preparation and completion, including alternative paths and handoffs.
