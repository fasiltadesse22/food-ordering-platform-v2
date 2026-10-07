# Checkpoint Manifest — C1.1-P03

## Identity

- Checkpoint: C1.1-P03
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P02
- Inherited checkpoint branch: checkpoints/C1.1-P02
- Inherited commit: 8ceb6046b44e5fa94574972496910cef95a8b02b
- P03 implementation commit: 3c1e6dcc4121b06552a7bc6778251d7dea664175
- P03 verification run: 37632251569

## Engineering question

When does requested intent become an accepted business fact?

## Prediction

An explicit command decision/result model can demonstrate that a requested PlaceOrder command may be accepted or rejected, and that OrderPlaced exists only on the accepted path, without introducing Kafka, an event bus, an outbox, event sourcing, or other messaging infrastructure.

## Source evolution

- added PlaceOrderResult Accepted/Rejected variants;
- added PlaceOrderRejection;
- added OrderPlaced local domain fact;
- evolved PlaceOrderUseCase return type;
- evolved PlaceOrderService decision path;
- evolved controller response mapping;
- removed obsolete ActorNotAllowedException;
- updated framework-free verification harness for result semantics.

## Test evolution

- inherited actor tests assert accepted/rejected results;
- service test asserts accepted result plus matching OrderPlaced fact;
- PlaceOrderSemanticFlowTest proves accepted vs rejected command behavior;
- inherited HTTP/domain/context tests remain green.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

The new semantic-flow tests passed and all inherited P01/P02 tests remained green.

## Architectural interpretation

This part introduces semantic domain facts, not distributed messaging.

It does not create:
- Kafka;
- event bus;
- outbox/inbox;
- event store;
- CQRS;
- Event Sourcing;
- integration-event contracts;
- service decomposition.

## Evidence-qualified guarantees

Within the tested application boundary:
- a valid PlaceOrder command can be accepted;
- accepted PlaceOrder updates current repository state;
- accepted PlaceOrder returns a corresponding OrderPlaced local fact;
- invalid actor PlaceOrder can be rejected;
- rejected actor command leaves current repository state unchanged;
- rejected result does not carry OrderPlaced.

Not guaranteed:
- caller authentication;
- durable fact history;
- fact publication;
- cross-process delivery;
- retry/duplicate safety;
- concurrency correctness;
- distributed consistency.

## Known non-guarantees intentionally preserved

- process restart loses current state;
- domain fact is not durably stored;
- fact is not published to other participants;
- duplicate commands are not handled;
- concurrency is not controlled;
- retry semantics are undefined;
- future command legality is not yet defined.

## Next pressure

Part 1.1.4 must deepen identity and authoritative-state reasoning: what entity is being referred to, which copy/state is authoritative, and what stale/transient representations mean.
