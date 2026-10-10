# Checkpoint Manifest — C1.1-P14 Candidate

## Identity

- Target checkpoint: C1.1-P14
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P13
- Inherited branch: checkpoints/C1.1-P13
- Inherited commit: 2f0bb04cc9975aec232aa5ea67eb32dd793789f2

## Engineering question

When customer cancellation and restaurant acceptance race from the same Order version, what should correctness mean, who is allowed to win, what can each participant know, and how do we distinguish concurrency-control mechanism from business conflict policy?

## Part classification

Type C — Cross-Actor Concurrency Failure Evolution.

## Production code/config

Unchanged.

The vulnerable:
find
→ decide
→ save

path remains preserved.

## Controlled experiment

Serial controls:
- cancel then accept;
- accept then cancel.

Shared-snapshot schedules:
- CANCELLED save first, ACCEPTED save last;
- ACCEPTED save first, CANCELLED save last.

## Minimal correctness requirement

The two mutually exclusive intents must not both be acknowledged as successful authoritative decisions from one logical PLACED version.

## Business policy

No winner policy is selected yet.

Documented alternatives:
- first authoritative commit wins;
- cancellation priority before a defined cutoff;
- acceptance priority after a fulfillment commitment.

Current last-write-wins is explicitly NOT approved policy.

## Mechanisms deliberately absent

- synchronized;
- version field;
- compare-and-set;
- optimistic locking;
- pessimistic locking;
- PostgreSQL;
- @Transactional;
- distributed lock;
- timestamp-winner rule.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P13 tests remain green;
3. both serial controls allow one success and reject the second;
4. both shared-snapshot schedules acknowledge both actors;
5. reversing save order reverses surviving authority;
6. participant-knowledge and policy artifacts are recorded;
7. no winner policy/concurrency mechanism is added without evidence.
