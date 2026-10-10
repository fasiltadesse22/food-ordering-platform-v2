# Checkpoint Manifest — C1.1-P16 Candidate

## Identity

- Target checkpoint: C1.1-P16
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P15
- Inherited branch: checkpoints/C1.1-P15
- Inherited commit: 076a684d4c26cc44526dbe6b9e6a721738b07c29

## Engineering question

What changes when a repeated command can cause an effect in another independently committed authority, where replacing local Order state cannot erase or merge that external effect?

## Part classification

Type C — Duplicate-Payment / External-Effect Failure Evolution.

## Production code/config

Unchanged.

PAYMENT_RECORDED remains a local learning-stage milestone, not a real payment-provider integration.

## Controlled experiments

1. repeated local recordPayment;
2. test-only external charge→local record repeated twice;
3. external charge commits but response is lost, followed by retry;
4. local record→external charge with provider failure before effect.

## Required distinctions

PAYMENT_RECORDED != external charge.

provider commit != provider response observed.

timeout/lost response != payment failure.

same final local state != exactly-once external effect.

local transaction/atomic object != remote-effect atomicity.

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

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P15 tests remain green;
3. repeated local payment produces two PAYMENT_RECORDED facts;
4. repeated charge→record produces two modeled external charges;
5. ambiguous first provider response + retry produces external charges=2 and local payment facts=1;
6. record→charge provider failure produces external charges=0 and local payment facts=1;
7. no production idempotency/distributed-payment mechanism is added prematurely.
