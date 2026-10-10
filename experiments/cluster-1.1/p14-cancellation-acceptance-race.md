# C1.1-P14 Experiment — Cancellation / Acceptance Race

## Question

When Customer CANCEL and Restaurant ACCEPT are both individually legal from PLACED, can the current application acknowledge both if they capture the same Order before either saves?

## Hypothesis

Because P13 proved find→evolve→save has no conflict detection, the same mechanism should apply across actors.

If both operations capture PLACED:
- CANCEL can derive CANCELLED;
- ACCEPT can derive ACCEPTED;
- both can return Accepted;
- last save determines current authority.

## Predictions

### Sequential cancel then accept

- cancellation accepted;
- acceptance reads CANCELLED;
- acceptance rejected as ILLEGAL_TRANSITION;
- final authority CANCELLED.

### Sequential accept then cancel

- acceptance accepted;
- cancellation reads ACCEPTED;
- cancellation rejected as ILLEGAL_TRANSITION;
- final authority ACCEPTED.

### Shared snapshot: cancel saves first

- both capture PLACED;
- both return Accepted;
- save order CANCELLED then ACCEPTED;
- final authority ACCEPTED;
- ORDER_CANCELLED absent from final current history.

### Shared snapshot: accept saves first

- both capture PLACED;
- both return Accepted;
- save order ACCEPTED then CANCELLED;
- final authority CANCELLED;
- RESTAURANT_ACCEPTED absent from final current history.

## Controlled variable

For the two concurrent experiments, only save order is reversed.

Actor identities, initial Order, clock, business methods and shared-snapshot barrier remain unchanged.

## Setup

Use real OrderWorkflowService and current domain rules.

Use a test-only coordinated OrderRepository implementing the real port.

No production concurrency mechanism is added.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P14 CI.

## Evidence interpretation

If predictions hold:
- the generic P13 stale-snapshot mechanism transfers to the cross-actor business race;
- current last-write-wins is not an acceptable proof of business winner policy;
- actor-local success does not guarantee surviving authoritative outcome.

## Limitations

Not measured:
- race frequency in real traffic;
- external restaurant work;
- external payment/refund consequence;
- multi-JVM behavior;
- persistence isolation;
- fairness/priority policy.

## Conclusion

Pending exact P14 execution evidence.
