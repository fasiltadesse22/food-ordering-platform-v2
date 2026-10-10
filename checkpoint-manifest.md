# Checkpoint Manifest — C1.1-P17

## Identity

- Checkpoint: C1.1-P17
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P16
- Inherited branch: checkpoints/C1.1-P16
- Inherited commit: ba40be8c21bab625bab215b683ba80dcb109ba23
- P17 experiment commit: 8d1366bc82034d56be9035b9bd86bcb93eca658c
- P17 verification run: 38034398881

## Engineering question

What happens when a decision was correct for the state we observed but is wrong, incomplete, or unsafe for the state that exists when we act?

## Part classification

Type C — Controlled Stale-State / Temporal-Correctness Failure Evolution.

## Production code/config

Unchanged.

## Verified experiments

1. stale pre-payment snapshot modified after payment:
   PAYMENT_RECORDED erased;

2. stale pre-cancellation snapshot paid after cancellation:
   ORDER_CANCELLED erased;
   lifecycle resurrected from CANCELLED to PLACED;

3. fresh payment after cancellation:
   status remains CANCELLED;
   ORDER_CANCELLED + PAYMENT_RECORDED coexist;
   recorded separately as current policy behavior;

4. fresh ACCEPTED versus stale PLACED modification:
   fresh guard rejects;
   stale guard passes;
   stale save erases acceptance and restores PLACED.

## Verification

GitHub Actions:
38034398881

Observed:
Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified distinctions

stale data != stale decision.

state valid when observed != state current when acted upon.

precondition true at read time != precondition guaranteed at commit time.

correct guard != freshness guarantee.

immutability != currentness.

simultaneous threads != required condition for stale-state failure.

missing business policy != stale-write corruption.

## Mechanisms deliberately absent

- version field;
- expected-version save;
- compare-and-set;
- optimistic locking;
- pessimistic locking;
- PostgreSQL;
- @Transactional;
- distributed coordination.

## Forward boundary

Part 1.1.18:
Process Crash, Transient State and Durability Non-Guarantees.
