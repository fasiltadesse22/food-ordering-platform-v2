# Checkpoint Manifest — C1.1-P11

## Identity

- Checkpoint: C1.1-P11
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P10
- Inherited branch: checkpoints/C1.1-P10
- Inherited commit: 61b80ae3628dfe7fe0c90f9071ef341ef7031d1f
- P11 implementation commit: fe4b60c99f195d4b5e8ac06095125df03afe8716
- P11 verification run: 37886764685

## Engineering question

Which related changes must be treated as one logical decision so that partial mutation cannot violate current Order invariants?

## Verified local model

Production:
- OrderEvolution binds next lifecycle state and workflow occurrence;
- normal Order evolution creates one immutable invariant-checked representation;
- one repository save publishes that complete Order as current in-process authority.

Experiment:
- status-first split update + failure is invariant-invalid;
- history-first split update + failure is invariant-invalid;
- both writes complete -> consistency restored;
- repository failure before replacement -> old PREPARING authority remains;
- successful replacement -> complete COMPLETED authority is visible.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Semantic distinctions

logical atomicity != speed.

atomicity != consistency.

atomicity != durability.

atomicity != concurrency control.

atomicity != distributed transaction.

transaction boundary != @Transactional annotation.

## Architectural interpretation

P11 discovers the transaction requirement before introducing a transaction mechanism.

The project still contains no:
- PostgreSQL;
- @Transactional;
- JDBC/JPA;
- transaction manager;
- optimistic/pessimistic locking;
- Saga/2PC;
- distributed coordination.

## Evidence-qualified guarantees

Within the tested one-process path:
- selected completion state+history are constructed together;
- selected split partial representations are detected as invariant-invalid;
- deterministic failure before current-authority replacement preserves old authority.

Not guaranteed:
- crash durability;
- concurrent read-modify-write atomicity;
- cross-Order atomicity;
- distributed atomicity;
- caller knowledge after response loss.

## Next pressure

Part 1.1.12 must distinguish requirement, implementation, observation and guarantee and classify every important P01-P11 correctness claim by evidence strength.
