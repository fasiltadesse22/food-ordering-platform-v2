# Checkpoint Manifest — C1.1-P06 Candidate

## Identity

- Target checkpoint: C1.1-P06
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P05
- Inherited branch: checkpoints/C1.1-P05
- Inherited commit: 2e63f12ea30fc211b6a5450654536415aaaa164d

## Engineering question

Which lifecycle transitions are valid, and which must be impossible?

## Prediction

A small explicit lifecycle model derived from P05 can preserve legitimate workflow branches while rejecting the contradictory ordering P05 intentionally allowed.

## Lifecycle states

- PLACED
- ACCEPTED
- REJECTED
- CANCELLED
- PREPARING
- COMPLETED

## Legal transitions introduced

- PLACED → ACCEPTED
- PLACED → REJECTED
- PLACED → CANCELLED
- ACCEPTED → PREPARING
- PREPARING → COMPLETED

Payment and refund remain orthogonal workflow milestones.

## Source evolution

- expanded OrderStatus;
- added OrderLifecycleTransition;
- added IllegalOrderTransitionException;
- evolved Order lifecycle methods to transition through explicit source/target rules;
- retained immutable Order evolution and workflow trace;
- retained OrderWorkflowService as the application coordinator.

## Test evolution

- added OrderStateMachineTest;
- evolved P05 workflow tests to assert real lifecycle state;
- evolved Spring workflow integration to end at COMPLETED;
- converted the P05 contradictory scenario into an illegal-transition regression.

## Architectural interpretation

P06 introduces lifecycle legality, not a generic workflow engine.

The project still contains no:
- state-machine framework;
- PostgreSQL;
- Kafka;
- Redis;
- Saga;
- Outbox;
- Event Sourcing;
- optimistic locking;
- distributed lock;
- service split.

## Evidence status

Pending P06 CI.

## Fragilities intentionally preserved

- no contextual guard beyond source state;
- no cancellation-after-acceptance policy;
- no explicit terminal/reversal semantics;
- no concurrency protection;
- no duplicate-command behavior;
- no durability.

## Next pressure

Part 1.1.7 must deepen guards, preconditions and postconditions: state source is necessary but may not be sufficient for deciding whether an operation is valid.

## Freeze gate

1. mvn -B -ntp verify succeeds;
2. inherited behavior remains green where still semantically valid;
3. legal transition tests pass;
4. illegal transition tests pass;
5. illegal attempts leave current state unchanged;
6. P05 contradiction is closed by executable evidence.
