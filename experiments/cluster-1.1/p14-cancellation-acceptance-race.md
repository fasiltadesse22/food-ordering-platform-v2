# C1.1-P14 Experiment — Cancellation / Acceptance Race

## Question

When Customer CANCEL and Restaurant ACCEPT are both individually legal from PLACED, can the current application acknowledge both if they capture the same Order before either saves?

## Execution

GitHub Actions run:
38027340494

Command:

mvn -B -ntp verify

## Observed test result

OrderCancellationAcceptanceRaceTest:
- tests run: 4
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 75
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Sequential cancel then accept — observed

- Customer CANCEL executed against PLACED and returned Accepted(CANCELLED).
- Restaurant ACCEPT performed a fresh read and observed CANCELLED.
- ACCEPT returned ILLEGAL_TRANSITION.
- final authority = CANCELLED.
- final current history = ORDER_CANCELLED.

## Sequential accept then cancel — observed

- Restaurant ACCEPT executed against PLACED and returned Accepted(ACCEPTED).
- Customer CANCEL performed a fresh read and observed ACCEPTED.
- CANCEL returned ILLEGAL_TRANSITION under the current lifecycle.
- final authority = ACCEPTED.
- final current history = RESTAURANT_ACCEPTED.

## Shared snapshot — cancellation saves first — observed

Both operations captured:
PLACED

Local decisions:
- Customer -> CANCELLED + ORDER_CANCELLED
- Restaurant -> ACCEPTED + RESTAURANT_ACCEPTED

Forced save order:
CANCELLED
then
ACCEPTED

Both application calls returned:
Accepted

Final authority:
ACCEPTED

Final current history:
RESTAURANT_ACCEPTED

ORDER_CANCELLED was absent from final current history.

## Shared snapshot — acceptance saves first — observed

Both operations again captured:
PLACED

Forced save order:
ACCEPTED
then
CANCELLED

Both calls returned:
Accepted

Final authority:
CANCELLED

Final current history:
ORDER_CANCELLED

RESTAURANT_ACCEPTED was absent from final current history.

## Evidence interpretation

Executed and verified:
- both valid serial histories permit only one successful mutually exclusive decision;
- the shared-snapshot race allows both actors to receive Accepted;
- last save determines current authority;
- reversing only save order reverses the surviving business outcome.

Evidence-backed inference:
- current implementation timing acts as an accidental winner policy;
- a future conflict-detection/serialization mechanism is needed if at-most-one authoritative acknowledgement is required;
- the mechanism cannot itself decide whether customer or restaurant has business priority.

## Business-policy status

Unresolved intentionally.

Candidate policies remain:
- first authoritative commit wins;
- cancellation priority before a defined business cutoff;
- acceptance priority after a defined fulfillment commitment.

No candidate is experimentally established as the correct business policy.

## Limitations

Not measured/implemented:
- race frequency in real traffic;
- external restaurant work;
- payment/refund side effects;
- multi-JVM execution;
- database isolation;
- fairness/priority policy;
- durable audit of both competing intents.

## Conclusion

P14 transfers P13's stale-snapshot conflict mechanism into the required customer-cancellation versus restaurant-acceptance race and demonstrates that current last-write-wins behavior cannot substitute for an explicit business conflict policy.
