# Checkpoint Manifest — C1.1-P05 Candidate

## Identity

- Target checkpoint: C1.1-P05
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P04
- Inherited branch: checkpoints/C1.1-P04
- Inherited commit: dfcea735542790ea4b424abdc6ae2f2dcde4a796

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

- happy path;
- restaurant rejection after recorded payment with refund-request handoff;
- cancellation before payment;
- cancellation after payment with refund-request handoff;
- contradictory/out-of-order sequence demonstrating missing transition guards;
- Spring integration proving workflow updates the same current Order.

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

## Evidence status

Pending P05 CI.

## Fragilities intentionally preserved

- any milestone ordering can currently be recorded;
- restaurant acceptance and rejection can both appear;
- preparation can be recorded before acceptance;
- completion has no formal prerequisites;
- payment/refund are local workflow representations only;
- current state remains transient.

## Next pressure

Part 1.1.6 must derive explicit lifecycle states and legal/illegal transitions from the P05 workflow evidence rather than inventing them independently.

## Freeze gate

1. root mvn -B -ntp verify succeeds;
2. inherited P01-P04 tests remain green;
3. new workflow path tests pass;
4. contradictory/out-of-order path is reproduced as evidence of missing state-machine guards;
5. evidence is recorded without calling that fragility a valid business path.
