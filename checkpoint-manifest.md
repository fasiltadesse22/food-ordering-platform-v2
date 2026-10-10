# Checkpoint Manifest — C1.1-P17 Candidate

## Identity

- Target checkpoint: C1.1-P17
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P16
- Inherited branch: checkpoints/C1.1-P16
- Inherited commit: ba40be8c21bab625bab215b683ba80dcb109ba23

## Engineering question

What happens when a decision was correct for the state we observed but is wrong, incomplete, or unsafe for the state that exists when we act?

## Part classification

Type C — Controlled Stale-State / Temporal-Correctness Failure Evolution.

## Production code/config

Unchanged.

## Controlled experiments

1. modification derived from pre-payment snapshot after payment becomes current;
2. payment derived from pre-cancellation snapshot after cancellation becomes current;
3. fresh payment-after-cancellation control;
4. fresh ACCEPTED guard versus stale PLACED guard.

## Required distinctions

stale data != stale decision.

state valid when observed != state current when acted upon.

precondition true at read time != precondition guaranteed at commit time.

correct guard != freshness guarantee.

immutability != currentness.

concurrent overlap != required condition for stale-state failure.

missing business policy != stale-state corruption.

## Mechanisms deliberately absent

- version field;
- expected-version save;
- compare-and-set;
- optimistic locking;
- pessimistic locking;
- PostgreSQL;
- @Transactional;
- distributed coordination.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P16 tests remain green;
3. stale modification after payment can erase PAYMENT_RECORDED;
4. stale payment after cancellation can erase ORDER_CANCELLED and resurrect PLACED;
5. fresh payment-after-cancellation behavior is recorded separately;
6. fresh ACCEPTED state rejects modification while stale PLACED copy permits it;
7. no version/locking mechanism is introduced prematurely.
