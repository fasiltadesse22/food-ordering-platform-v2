# Checkpoint Manifest — C1.1-P13 Candidate

## Identity

- Target checkpoint: C1.1-P13
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P12
- Inherited branch: checkpoints/C1.1-P12
- Inherited commit: 0d81069ea1761c2e88825bff6d3dcd6c803f4879

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

path is preserved.

## Controlled experiment

Restaurant ACCEPT versus Restaurant REJECT.

Sequential control:
second decision must observe fresh authority and reject.

Concurrent schedules:
both operations capture the same PLACED snapshot before either save.

Schedule A:
ACCEPT saves first, REJECT saves last.

Schedule B:
REJECT saves first, ACCEPT saves last.

## Expected failure property

Both calls can report Accepted even though only the last successor remains current authority.

Final state changes when only save ordering is reversed.

## Mechanisms deliberately absent

- synchronized;
- locks;
- versions;
- compare-and-set;
- optimistic locking;
- pessimistic locking;
- PostgreSQL;
- @Transactional;
- distributed locks.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P12 tests remain green;
3. sequential control rejects the second conflicting action;
4. shared-snapshot concurrent schedule makes both calls return Accepted;
5. final authority follows deterministic last-save order;
6. reverse save order reverses surviving decision;
7. no concurrency protection is added prematurely.
