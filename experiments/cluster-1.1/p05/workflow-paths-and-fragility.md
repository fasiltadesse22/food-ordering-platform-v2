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

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P05 GitHub Actions run.

## Evidence interpretation rule

If the suspicious-sequence test passes, record that as evidence of a missing legality mechanism.

Do not report it as a valid business guarantee.

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

Pending execution evidence.
