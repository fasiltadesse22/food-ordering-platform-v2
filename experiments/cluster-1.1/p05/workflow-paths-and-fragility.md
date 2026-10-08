# C1.1-P05 Experiment — Workflow Paths and Pre-State-Machine Fragility

## Question

Can the application represent the discovered end-to-end workflow branches and handoffs without silently pretending that lifecycle legality has already been solved?

## Hypothesis

A permissive workflow trace can reproduce happy, rejection and cancellation branches, while also exposing contradictory/out-of-order sequences that demonstrate the need for a later state machine.

## Predictions

1. Happy scenario records payment, restaurant acceptance, preparation and completion in order.
2. Restaurant rejection after payment exposes a refund-request handoff.
3. Cancellation before payment records a distinct branch without a payment/refund milestone.
4. Cancellation after payment can record refund-request pressure.
5. A suspicious sequence can still be recorded because P05 intentionally has no transition guards.
6. Order.status remains PLACED even after multiple workflow milestones, proving workflow progress is not yet a formal lifecycle state machine.

## Setup

Executable tests:

- OrderWorkflowDiscoveryTest
- OrderWorkflowSpringIntegrationTest

The application uses:
- the verified P04 current-state repository;
- OrderWorkflowUseCase;
- OrderWorkflowService;
- immutable Order workflow occurrences;
- one Spring Boot process.

## Controlled variables

Tests vary the milestone sequence while keeping:
- the same application topology;
- in-memory persistence;
- no network calls;
- no database;
- no external payment side effect.

## Execution

GitHub Actions run: 37738287058

Command:

mvn -B -ntp verify

## Observation

OrderWorkflowDiscoveryTest:
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
- tests run: 21
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed happy path:
PAYMENT_RECORDED
→ RESTAURANT_ACCEPTED
→ PREPARATION_STARTED
→ ORDER_COMPLETED

Observed rejection path:
PAYMENT_RECORDED
→ RESTAURANT_REJECTED
→ REFUND_REQUESTED

Observed cancellation-before-payment branch:
ORDER_CANCELLED

Observed cancellation-after-payment branch:
PAYMENT_RECORDED
→ ORDER_CANCELLED
→ REFUND_REQUESTED

Observed pre-state-machine fragility:
PREPARATION_STARTED
→ RESTAURANT_ACCEPTED
→ RESTAURANT_REJECTED
→ ORDER_COMPLETED

The suspicious sequence was accepted by the P05 recorder and Order.status remained PLACED.

## Interpretation

The project can now execute multiple workflow paths and participant handoffs.

The contradictory path passing is evidence that workflow discovery alone does not enforce lifecycle correctness.

It is therefore pressure for P06, not a business guarantee.

## Limitations

The experiment does not establish:
- final lifecycle states;
- legal or illegal transitions;
- guards/preconditions;
- terminality;
- real payment/refund effects;
- concurrency correctness;
- duplicate-command handling;
- distributed workflow coordination.

## Conclusion

P05 successfully makes the workflow executable while preserving the missing-state-machine fragility required for the next part.
