# Checkpoint Manifest — C1.1-P04 Candidate

## Identity

- Target checkpoint: C1.1-P04
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P03
- Inherited branch: checkpoints/C1.1-P03
- Inherited commit: 5806426484a646a27df26955d56021e29a1a9f14

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

OrderIdentityAndAuthorityTest covers:
- equal identifier values across different OrderId objects;
- same business identity across different Order Java instances;
- repository selection of current same-identity representation;
- detached snapshot becoming stale;
- unknown identity lookup.

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

## Evidence status

Pending P04 CI.

## Non-guarantees deliberately preserved

- process restart loses authority state;
- repository overwrite is concurrency-naive;
- stale writes are not detected;
- lifecycle-valid update rules are not defined;
- business identity is not yet backed by a database uniqueness constraint.

## Next pressure

Part 1.1.5 will move from isolated concepts into end-to-end workflow discovery: placement, payment, restaurant decision, cancellation, refund, preparation and completion, while still avoiding premature distribution.

## Freeze gate

1. root mvn -B -ntp verify succeeds;
2. inherited P01-P03 tests remain green;
3. OrderIdentityAndAuthorityTest passes;
4. stale-snapshot behavior is observed rather than merely asserted in prose;
5. no concurrency or database mechanism is introduced prematurely.
