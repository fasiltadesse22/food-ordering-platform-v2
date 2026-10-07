# Checkpoint Manifest — C1.1-P02

## Identity

- Checkpoint: `C1.1-P02`
- Current status: **VERIFIED AND FROZEN**
- Inherited checkpoint: `C1.1-P01`
- Inherited checkpoint branch: `checkpoints/C1.1-P01`
- Inherited commit: `d6dbc93e06955e5e7d288ac6a61093e16ce09d71`
- P02 implementation commit: `a8fbe96e179cc3ff5aadb07d483c6130e4e42de3`
- P02 verification run: `37629067834`

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

## Verification

GitHub Actions executed:

```bash
mvn -B -ntp verify
```

Observed:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The three P02 actor acceptance tests passed, and all inherited P01 tests remained green.

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

## Evidence-qualified guarantees

Within the tested application boundary:

- modeled customer actor may place for the same customer identity;
- restaurant operator actor is rejected from Place Order;
- mismatched customer actor is rejected before persistence.

Not guaranteed:

- caller authentication;
- general authorization policy;
- durability;
- concurrency correctness;
- duplicate safety;
- distributed consistency.

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

## Next pressure

Part 1.1.3 must answer when actor intent becomes a command, how a decision accepts/rejects it, and when an accepted result becomes a domain fact.
