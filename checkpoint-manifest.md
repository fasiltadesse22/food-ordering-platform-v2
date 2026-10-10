# Checkpoint Manifest — C1.1-P16

## Identity

- Checkpoint: C1.1-P16
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P15
- Inherited branch: checkpoints/C1.1-P15
- Inherited commit: 076a684d4c26cc44526dbe6b9e6a721738b07c29
- P16 experiment commit: d6184d0ce7b31b9cf6524a025b901e28e2d94b78
- P16 verification run: 38032714055

## Engineering question

What changes when a repeated command can cause an effect in another independently committed authority, where replacing local Order state cannot erase or merge that external effect?

## Part classification

Type C — Duplicate-Payment / External-Effect Failure Evolution.

## Production code/config

Unchanged.

PAYMENT_RECORDED remains a local learning-stage milestone, not a real payment-provider integration.

## Verified experiments

1. repeated local recordPayment:
   two Accepted results and two PAYMENT_RECORDED facts;

2. repeated external charge→local record:
   external charges = 2;
   local payment facts = 2;

3. external charge commits + response lost + retry:
   external charges = 2;
   local payment facts = 1;

4. local record→external provider failure:
   external charges = 0;
   local payment facts = 1.

## Verification

GitHub Actions:
38032714055

Observed:
Tests run: 82, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified distinctions

PAYMENT_RECORDED != external charge.

provider commit != provider response observed.

timeout/lost response != payment failure.

same local history != exactly-once external effect.

local immutable/atomic evolution != remote-effect atomicity.

call ordering != distributed atomicity.

reversal != compensation.

## Mechanisms deliberately absent

- payment ID/idempotency key;
- provider-side idempotency;
- durable payment entity;
- dedup store;
- stored result replay;
- distributed transaction;
- Saga;
- Outbox;
- Kafka;
- automated reconciliation.

## Forward boundary

Part 1.1.17:
Stale-State Decisions and Temporal Correctness.
