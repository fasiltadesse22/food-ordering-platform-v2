# Checkpoint Manifest — C1.1-P08 Candidate

## Identity

- Target checkpoint: C1.1-P08
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P07
- Inherited branch: checkpoints/C1.1-P07
- Inherited commit: 6f4ca2c2d073575f6eaa831e0031d6f7205e124e

## Engineering question

Which lifecycle outcomes can truly be undone, which cannot, and when must the business perform a new compensating action instead of pretending the original fact never occurred?

## Current terminal classification

Terminal:
- REJECTED
- CANCELLED
- COMPLETED

Nonterminal:
- PLACED
- ACCEPTED
- PREPARING

## Compensation semantics

- PAYMENT_RECORDED is retained as historical truth;
- REFUND_REQUESTED is a new compensating workflow occurrence;
- refund request does not reopen or change terminal OrderStatus;
- second refund request is rejected in the current semantic model.

## Modification semantics

- real ModifyOrderUseCase added;
- owning customer may modify while PLACED;
- terminal Orders cannot be modified;
- modification keeps OrderStatus PLACED and appends ORDER_MODIFIED.

## Important preserved fragility

A paid Order remains PLACED and can therefore still be modified.

P08 intentionally does not solve modification-after-payment.

## Evidence status

Pending P08 CI.

## Mechanisms deliberately absent

- Saga;
- rollback coordinator;
- external refund provider;
- Event Sourcing;
- PostgreSQL;
- Kafka;
- Redis;
- optimistic locking;
- distributed lock.

## Freeze gate

1. root Maven verification succeeds;
2. inherited tests remain green;
3. terminal state classification is verified;
4. refund compensation history is verified;
5. duplicate semantic refund is rejected;
6. cancel-completed and modify-terminal tests pass;
7. ModifyOrderUseCase works for PLACED Orders.
