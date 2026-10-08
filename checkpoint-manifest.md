# Checkpoint Manifest — C1.1-P05

## Identity

- Checkpoint: C1.1-P05
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P04
- Inherited branch: checkpoints/C1.1-P04
- Inherited commit: dfcea735542790ea4b424abdc6ae2f2dcde4a796
- P05 implementation commit: fa37c3b0cff91e3812cc0d6fa10373c9d46a9daf
- P05 verification run: 37738287058

## Engineering question

What actually happens from placement to a business outcome, including alternative paths, handoffs, cancellation, refund, preparation and completion?

## Prediction

A permissive workflow recorder can make end-to-end paths executable while preserving the distinction workflow != state machine and creating evidence for P06 transition legality.

## Source evolution

- added workflow participant/action/occurrence model;
- evolved Order to record post-placement workflow occurrences;
- added OrderWorkflowUseCase;
- added OrderWorkflowService;
- evolved OrderSnapshot to expose workflow trace;
- wired workflow service into the Spring application.

## Executable scenarios

Verified:
- happy path;
- restaurant rejection after recorded payment with refund-request handoff;
- cancellation before payment;
- cancellation after payment with refund-request handoff;
- contradictory/out-of-order sequence demonstrating missing transition guards;
- Spring integration proving workflow updates the same current Order.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

All inherited P01-P04 tests remained green.

## Architectural interpretation

This part records workflow milestones, not final lifecycle state.

Order.status intentionally remains PLACED.

The project still contains no:
- PostgreSQL;
- Kafka;
- Redis;
- Saga;
- Outbox;
- CQRS;
- Event Sourcing;
- state-machine library;
- optimistic locking;
- service decomposition.

## Evidence-qualified guarantees

Within the tested one-process learning model:
- selected workflow branches are reproducible;
- participant handoffs are explicit;
- current workflow progress is observable;
- suspicious sequencing is not currently rejected.

Not guaranteed:
- business validity of every recorded sequence;
- external payment/refund effects;
- transition legality;
- durability;
- concurrency correctness;
- duplicate safety.

## Fragilities intentionally preserved

- restaurant acceptance and rejection can both appear;
- preparation can be recorded before acceptance;
- completion has no formal prerequisites;
- current status remains PLACED;
- payment/refund are local representations only;
- current state remains transient.

## Next pressure

Part 1.1.6 must derive explicit lifecycle states and legal/illegal transitions from the P05 workflow evidence rather than inventing them independently.
