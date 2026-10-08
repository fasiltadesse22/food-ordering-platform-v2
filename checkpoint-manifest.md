# Checkpoint Manifest — C1.1-P10

## Identity

- Checkpoint: C1.1-P10
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P09
- Inherited branch: checkpoints/C1.1-P09
- Inherited commit: 7ed3b6dd8862a0f0162f63f234fea8a2163a1bf6
- P10 implementation commit: 3052ed76d4ceca0093baafe0b7144acc44842638
- P10 verification run: 37791411073

## Engineering question

Which rules define valid business state, which only validate input shape, and which are persistence enforcement mechanisms?

## Verified rule layers

Boundary validation:
- Bean Validation rejects malformed HTTP request shape before use-case/domain evaluation.

Domain structural validation:
- CustomerId, RestaurantId and OrderLine defend structural validity outside HTTP.

Business invariants:
- OrderInvariants verifies state/history coherence whenever an Order representation is constructed/evolved.

Future database constraints:
- documented only;
- not executed or claimed.

## Verified distinctions

Validation != invariant.

Invariant != database constraint.

Guard/precondition != invariant.

A structurally valid set of facts can still violate an invariant.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Architectural interpretation

P10 establishes semantic rule ownership before persistence is introduced.

The project still contains no:
- PostgreSQL;
- migrations;
- JDBC/JPA;
- @Transactional;
- Testcontainers database;
- optimistic locking;
- distributed coordination.

## Evidence-qualified guarantees

Within the tested one-process model:
- selected structural request/value validation is enforced;
- selected Order state/history invariants are enforced;
- normal lifecycle behavior prevents invariant-invalid construction.

Not guaranteed:
- durable database enforcement;
- concurrent invariant preservation;
- atomic multi-change commit;
- stale-state safety.

## Next pressure

Part 1.1.11 must determine which related changes belong to one logical decision and demonstrate what partial mutation/failure would mean before real database transactions arrive.
