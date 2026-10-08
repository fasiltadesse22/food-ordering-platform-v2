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

## Execution

GitHub Actions run: 37744525908

Command:

mvn -B -ntp verify

## Observation

OrderStateMachineTest:
- tests run: 7
- failures: 0
- errors: 0
- skipped: 0

Evolved OrderWorkflowDiscoveryTest:
- tests run: 5
- failures: 0
- errors: 0
- skipped: 0

OrderWorkflowSpringIntegrationTest:
- tests run: 1
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 28
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed legal fulfillment path:
PLACED → ACCEPTED → PREPARING → COMPLETED

Observed legal branches:
PLACED → REJECTED
PLACED → CANCELLED

Observed orthogonal milestone:
PAYMENT_RECORDED leaves OrderStatus at PLACED.

Observed illegal transitions:
- START_PREPARATION from PLACED rejected;
- REJECT from ACCEPTED rejected;
- COMPLETE from ACCEPTED rejected.

The evolved P05 regression verifies that preparation-before-acceptance is rejected before repository replacement or workflow append.

## Interpretation

P06 closes the specific lifecycle-ordering fragility demonstrated by P05.

The state machine now enforces source-state legality.

## Limitations

This does not yet establish:
- actor/contextual guards;
- payment-dependent preconditions;
- terminality;
- reversibility/compensation;
- concurrency correctness;
- stale-write protection;
- repeated-command semantics.

## Conclusion

The evidence supports explicit lifecycle-state legality while preserving the remaining pressures for P07 and later parts.
