# C1.1-P08 Experiment — Terminality, Irreversibility and Compensation

## Question

Can the project distinguish terminal lifecycle outcomes from later compensating workflow actions while preserving historical facts rather than pretending to roll them back?

## Hypothesis

If P08 semantics are correct:
- REJECTED, CANCELLED and COMPLETED are explicitly terminal for OrderStatus;
- refund after paid cancellation preserves PAYMENT_RECORDED and adds REFUND_REQUESTED;
- second refund request is rejected without erasing the first;
- completed Order cannot be cancelled;
- terminal cancelled Order cannot be modified;
- PLACED Order can be modified without changing lifecycle status.

## Predictions

1. OrderStatus terminal flags match the current lifecycle graph.
2. PAYMENT_RECORDED → ORDER_CANCELLED → REFUND_REQUESTED remains fully visible.
3. second refund request fails with REFUND_ALREADY_REQUESTED.
4. CANCEL from COMPLETED fails as illegal transition.
5. modification from CANCELLED fails.
6. modification from PLACED succeeds and appends ORDER_MODIFIED.
7. no Saga/distributed rollback mechanism is required for these local semantics.

## Setup

Executable tests:
- OrderTerminalityAndCompensationTest
- ModifyOrderUseCaseTest
- all inherited tests.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P08 CI.

## Limitations

The experiment does not prove:
- a real payment was charged;
- a real refund executed;
- physical preparation actually occurred;
- compensation is durable;
- repeated refund retry is idempotent;
- modification after payment is safe.

## Conclusion

Pending execution evidence.
