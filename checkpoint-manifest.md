# Checkpoint Manifest — C1.1-P06

## Identity

- Checkpoint: C1.1-P06
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P05
- Inherited branch: checkpoints/C1.1-P05
- Inherited commit: 2e63f12ea30fc211b6a5450654536415aaaa164d
- P06 implementation commit: 697fb55ab5bda075ed38840d3241d3c4aea1de02
- P06 verification run: 37744525908

## Engineering question

Which lifecycle transitions are valid, and which must be impossible?

## Lifecycle states

- PLACED
- ACCEPTED
- REJECTED
- CANCELLED
- PREPARING
- COMPLETED

## Verified legal transitions

- PLACED → ACCEPTED
- PLACED → REJECTED
- PLACED → CANCELLED
- ACCEPTED → PREPARING
- PREPARING → COMPLETED

Payment/refund remain orthogonal workflow milestones.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified illegal examples

- PLACED → START_PREPARATION rejected;
- ACCEPTED → REJECT rejected;
- ACCEPTED → COMPLETE rejected.

Rejected attempts leave the existing immutable Order representation unchanged and do not append the attempted workflow occurrence.

## Architectural interpretation

P06 introduces explicit lifecycle legality, not a workflow engine or state-machine framework.

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

## Evidence-qualified guarantees

Within the tested one-process model:
- selected legal transitions succeed;
- selected illegal transitions fail before current-state replacement;
- P05 preparation-before-acceptance fragility is closed;
- payment can be recorded while lifecycle remains PLACED.

Not guaranteed:
- rich contextual guards;
- terminal/reversal semantics;
- concurrency correctness;
- stale-write rejection;
- repeated-command safety;
- durability.

## Next pressure

Part 1.1.7 deepens guards, preconditions and postconditions: a source state can be necessary for a transition without being sufficient to authorize it.
