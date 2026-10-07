# Checkpoint Manifest — C1.1-P02 Candidate

## Identity

- Target checkpoint: `C1.1-P02`
- Current status: **CANDIDATE — NOT FROZEN**
- Inherited checkpoint: `C1.1-P01`
- Inherited checkpoint branch: `checkpoints/C1.1-P01`
- Inherited commit: `d6dbc93e06955e5e7d288ac6a61093e16ce09d71`

## Engineering question

Who is trying to accomplish what, and which responsibility belongs to whom?

## Prediction

Explicitly representing the caller's business actor at the Place Order application boundary will allow executable responsibility checks while preserving one deployable and avoiding authentication/security infrastructure or service decomposition.

## Changes

### Source

- added `ActorType`;
- added `ActorContext`;
- evolved `PlaceOrderCommand` to carry actor context;
- preserved the previous constructor so the verified P01 HTTP/core path remains compatible;
- evolved `PlaceOrderService` to reject actor/use-case responsibility violations;
- added `ActorNotAllowedException`.

### Tests

- customer actor can execute Place Order for the same customer;
- restaurant operator cannot execute customer Place Order;
- customer actor cannot place for a different customer identity.

### Architecture learning assets

- actor/goal/responsibility/use-case catalog;
- responsibility matrix;
- actor/use-case diagram;
- invalid-actor controlled experiment;
- P02 evidence ledger.

## Architectural interpretation

This part creates **semantic responsibility**, not runtime decomposition.

It does not create:

- Customer Service;
- Restaurant Service;
- Payment Service;
- authentication;
- authorization infrastructure;
- database-per-service;
- Kafka or other remote communication.

## Evidence status

Pending P02 CI execution.

## Fragilities intentionally preserved

- transient in-memory state;
- no durability guarantee;
- no DB transaction/locking behavior;
- no duplicate-command mechanism;
- no concurrency protection;
- no retries;
- no distributed boundaries.

## Deferred questions

- command vs fact semantics → Part 1.1.3;
- identity/authoritative-state depth → Part 1.1.4;
- lifecycle/state transitions → later Chapter II parts;
- service decomposition → Cluster 1.4.

## Freeze gate

1. root `mvn -B -ntp verify` succeeds;
2. existing P01 tests remain green;
3. new actor acceptance tests execute and pass;
4. CI evidence is recorded;
5. no future mechanism has been introduced without authorization.
