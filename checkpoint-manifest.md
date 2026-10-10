# Checkpoint Manifest — C1.1-P14

## Identity

- Checkpoint: C1.1-P14
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P13
- Inherited branch: checkpoints/C1.1-P13
- Inherited commit: 2f0bb04cc9975aec232aa5ea67eb32dd793789f2
- P14 experiment commit: ea9581114b21b4331c8776681f5311e7ffaa3175
- P14 verification run: 38027340494

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

## Verified serial controls

Cancel first:
- cancellation Accepted;
- later acceptance rejected;
- final authority CANCELLED.

Accept first:
- acceptance Accepted;
- later cancellation rejected;
- final authority ACCEPTED.

## Verified shared-snapshot race

Schedule A:
- both capture PLACED;
- both return Accepted;
- CANCELLED saves first;
- ACCEPTED saves last;
- final authority ACCEPTED.

Schedule B:
- both capture PLACED;
- both return Accepted;
- ACCEPTED saves first;
- CANCELLED saves last;
- final authority CANCELLED.

## Minimal correctness requirement

The two mutually exclusive intents must not both be acknowledged as successful authoritative decisions from one logical PLACED version.

## Business policy

No winner policy is selected yet.

Documented alternatives:
- first authoritative commit wins;
- cancellation priority before a defined cutoff;
- acceptance priority after a fulfillment commitment.

Current last-write-wins is explicitly NOT approved business policy.

## Verification

GitHub Actions:
38027340494

Observed:
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified distinctions

concurrency-control mechanism != business winner policy.

actor-local success != guaranteed surviving authority.

request intent time != authoritative commit order.

valid individual successor != valid concurrent acknowledgement history.

current last-write-wins behavior != justified conflict policy.

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

## Next pressure

Part 1.1.15 studies repeated commands and replay semantics without conflating duplicates with cross-actor conflicts.
