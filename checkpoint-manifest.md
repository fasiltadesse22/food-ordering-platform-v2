# Checkpoint Manifest — C1.1-P11 Candidate

## Identity

- Target checkpoint: C1.1-P11
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P10
- Inherited branch: checkpoints/C1.1-P10
- Inherited commit: 61b80ae3628dfe7fe0c90f9071ef341ef7031d1f

## Engineering question

Which related changes must be treated as one logical decision so that partial mutation cannot violate the current Order invariants?

## Project changes

Production:
- OrderEvolution explicitly binds next lifecycle state and workflow occurrence;
- normal Order evolution constructs one immutable invariant-checked representation;
- repository documentation states exact local publication boundary and non-guarantees.

Experiments:
- status-first split completion with injected failure;
- history-first split completion with injected failure;
- successful split completion after both writes;
- repository failure before authority replacement;
- successful complete authority replacement.

## Semantic distinctions

logical atomicity != speed.

atomicity != consistency.

atomicity != durability.

atomicity != concurrency control.

atomicity != distributed transaction.

transaction boundary != @Transactional annotation.

## Evidence status

Pending P11 CI.

## Mechanisms deliberately absent

- PostgreSQL;
- @Transactional;
- JDBC/JPA;
- database transaction manager;
- optimistic/pessimistic locking;
- Saga/2PC;
- distributed coordination.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P10 tests remain green;
3. normal completion publishes a complete state+fact representation;
4. status-first split failure is observed as invariant-invalid;
5. history-first split failure is observed as invariant-invalid;
6. both split writes restore consistency;
7. failed repository replacement leaves old authority unchanged;
8. successful replacement publishes complete new authority.
