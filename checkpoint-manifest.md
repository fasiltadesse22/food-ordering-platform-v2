# Checkpoint Manifest — C1.1-P13

## Identity

- Checkpoint: C1.1-P13
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P12
- Inherited branch: checkpoints/C1.1-P12
- Inherited commit: 0d81069ea1761c2e88825bff6d3dcd6c803f4879
- P13 experiment commit: 198ded2738a9a68192cd0530c52224ef41611e6f
- P13 verification run: 38026585059

## Engineering question

How can two individually legal business decisions conflict when both are made from the same previously authoritative state?

## Part classification

Type C — Controlled Concurrency Failure Evolution.

## Production code/config

Unchanged.

The current vulnerable:
find
→ evolve
→ save

path remains preserved.

## Verified experiment

Sequential:
- ACCEPT succeeds;
- fresh REJECT observes ACCEPTED and is rejected.

Concurrent schedule A:
- both capture PLACED;
- both return Accepted;
- ACCEPT saves first;
- REJECT saves last;
- final authority = REJECTED.

Concurrent schedule B:
- both capture PLACED;
- both return Accepted;
- REJECT saves first;
- ACCEPT saves last;
- final authority = ACCEPTED.

## Verification

GitHub Actions:
38026585059

Observed:
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified distinctions

locally legal decision != globally correct concurrent history.

immutable successor != protected stale replacement.

thread-safe map operation != atomic business read-modify-write.

Accepted application result != guaranteed durable/current surviving decision.

scheduler-dependent race testing != deterministic interleaving testing.

## Mechanisms deliberately absent

- synchronized;
- lock;
- version field;
- compare-and-set;
- optimistic locking;
- pessimistic locking;
- PostgreSQL;
- @Transactional;
- distributed lock.

## Evidence-qualified non-guarantee

Conflicting Order updates are not currently serialized or conflict-detected across find→evolve→save.

## Next pressure

Part 1.1.14 applies the now-proven conflict mechanism to Customer CANCEL versus Restaurant ACCEPT.
