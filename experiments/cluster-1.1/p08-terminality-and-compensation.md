# C1.1-P08 Experiment — Terminality, Irreversibility and Compensation

## Question

Can the project distinguish terminal lifecycle outcomes from later compensating workflow actions while preserving historical facts rather than pretending to roll them back?

## Execution

GitHub Actions run: 37783525459

Command:

mvn -B -ntp verify

## Observation

OrderTerminalityAndCompensationTest:
- tests run: 6
- failures: 0
- errors: 0
- skipped: 0

ModifyOrderUseCaseTest:
- tests run: 2
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 44
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed:
- REJECTED, CANCELLED and COMPLETED are explicitly terminal for OrderStatus;
- PLACED, ACCEPTED and PREPARING are nonterminal;
- PAYMENT_RECORDED → ORDER_CANCELLED → REFUND_REQUESTED remains fully visible;
- refund request does not change CANCELLED status;
- second refund request is rejected with REFUND_ALREADY_REQUESTED;
- COMPLETED cannot transition through CANCEL;
- terminal CANCELLED Order cannot be modified;
- PLACED Order can be modified, keeps PLACED status and appends ORDER_MODIFIED;
- ModifyOrder application use case replaces repository current state only on successful modification;
- failed terminal modification preserves repository authority.

## Interpretation

Terminality concerns Order lifecycle transitions.

Compensation is a later action that may occur after terminality.

Refund request preserves payment history rather than erasing it.

No exact rollback of payment/preparation/completion history is modeled.

## Important preserved fragility

PAYMENT_RECORDED leaves OrderStatus PLACED.

Therefore ModifyOrder remains possible after payment if the Order is still PLACED.

P08 intentionally preserves this for later modification-after-payment/stale-decision work.

## Limitations

The experiment does not prove:
- real payment execution;
- real refund execution;
- physical kitchen irreversibility;
- distributed compensation;
- idempotent refund retry;
- durability.

## Conclusion

P08 successfully establishes local terminality and compensation semantics without introducing distributed rollback or Saga.
