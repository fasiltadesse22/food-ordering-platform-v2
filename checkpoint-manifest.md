# Checkpoint Manifest — C1.1-P10 Candidate

## Identity

- Target checkpoint: C1.1-P10
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P09
- Inherited branch: checkpoints/C1.1-P09
- Inherited commit: 7ed3b6dd8862a0f0162f63f234fea8a2163a1bf6

## Engineering question

Which rules define valid business state, which only validate input shape, and which are persistence enforcement mechanisms?

## Project changes

Boundary validation:
- Bean Validation on PlaceOrderRequest and CancelOrderRequest;
- malformed HTTP request shape maps to 400 before use-case/domain evaluation.

Domain structural validation:
- existing CustomerId, RestaurantId and OrderLine validation remains active for non-HTTP callers.

Business invariants:
- OrderInvariants verifies current state/history coherence on each Order construction/evolution;
- impossible internal combinations raise OrderInvariantViolationException.

Future database constraints:
- documented only;
- no schema/database introduced in P10.

## Semantic classification

Validation != invariant.

Invariant != database constraint.

Guard/precondition != invariant.

A database constraint may reinforce a semantic rule but does not become the semantic definition of that rule.

## Evidence status

Pending P10 CI.

## Mechanisms deliberately absent

- PostgreSQL;
- migrations;
- @Transactional;
- JDBC/JPA;
- Testcontainers database;
- optimistic locking;
- distributed coordination.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P09 tests remain green;
3. HTTP boundary validation tests pass;
4. domain structural validation tests pass;
5. structurally valid but invariant-invalid state/history is rejected by OrderInvariants;
6. valid terminal compensation history remains invariant-valid;
7. public lifecycle behavior still prevents invariant violation before construction.
