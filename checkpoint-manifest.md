# Checkpoint Manifest — C1.1-P03 Candidate

## Identity

- Target checkpoint: C1.1-P03
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P02
- Inherited checkpoint branch: checkpoints/C1.1-P02
- Inherited commit: 8ceb6046b44e5fa94574972496910cef95a8b02b

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

- inherited actor tests now assert accepted/rejected results;
- service test asserts accepted result plus matching OrderPlaced fact;
- new semantic-flow test proves accepted vs rejected command behavior;
- inherited HTTP/domain/context tests remain part of the root verification gate.

## Learning assets

- intent/command/decision/fact scenario;
- candidate command/fact catalog;
- semantic-flow diagram;
- ADR-0003 domain facts without messaging;
- controlled P03 experiment;
- P03 evidence ledger.

## Architectural interpretation

This part introduces semantic facts, not distributed messaging.

It does not create:
- Kafka;
- event bus;
- outbox/inbox;
- event store;
- CQRS;
- Event Sourcing;
- integration-event contracts;
- service decomposition.

## Evidence status

Pending P03 CI execution.

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

## Freeze gate

1. mvn -B -ntp verify succeeds;
2. inherited tests remain green;
3. PlaceOrderSemanticFlowTest passes;
4. accepted result/fact correspondence is observed;
5. rejected command leaves repository unchanged;
6. no messaging mechanism has been introduced without evidence.
