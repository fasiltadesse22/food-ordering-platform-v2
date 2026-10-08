# C1.1-P06 Experiment — Legal and Illegal Lifecycle Transitions

## Question

Can an explicit lifecycle state machine preserve the P05 happy/alternative workflows while rejecting contradictory ordering that P05 previously accepted?

## Hypothesis

If lifecycle transitions are modeled explicitly:
- the selected fulfillment path will evolve PLACED → ACCEPTED → PREPARING → COMPLETED;
- rejection and cancellation will branch from PLACED;
- payment will remain an orthogonal milestone and leave OrderStatus unchanged;
- preparation from PLACED, rejection after ACCEPTED and completion before PREPARING will be rejected;
- an illegal transition will not mutate current Order state or append the attempted workflow occurrence.

## Predictions

1. PLACED → ACCEPTED → PREPARING → COMPLETED succeeds.
2. PLACED → REJECTED succeeds.
3. PLACED → CANCELLED succeeds.
4. PAYMENT_RECORDED leaves lifecycle state PLACED.
5. START_PREPARATION from PLACED throws IllegalOrderTransitionException.
6. REJECT from ACCEPTED throws IllegalOrderTransitionException.
7. COMPLETE from ACCEPTED throws IllegalOrderTransitionException.
8. The P05 preparation-before-acceptance fragility is no longer recordable.

## Setup

Executable tests:
- OrderStateMachineTest
- evolved OrderWorkflowDiscoveryTest
- evolved OrderWorkflowSpringIntegrationTest

## Controlled variables

The tests vary:
- current OrderStatus;
- attempted transition.

They keep:
- one JVM;
- the same in-memory repository;
- no database;
- no concurrent writers;
- no real external payment;
- no retries.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P06 CI.

## Evidence interpretation

A rejected illegal transition demonstrates state-source legality.

It does not yet demonstrate:
- actor authorization;
- payment-dependent guards;
- temporal deadlines;
- concurrency correctness;
- terminal/reversal semantics.

## Conclusion

Pending execution evidence.
