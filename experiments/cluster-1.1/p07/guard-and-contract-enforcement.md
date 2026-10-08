# C1.1-P07 Experiment — Guard and Contract Enforcement

## Question

Can the application demonstrate that a structurally legal transition may still be rejected by contextual preconditions, and that successful operations satisfy explicit postconditions?

## Fragile baseline

C1.1-P06 accepted restaurant/customer lifecycle operations without carrying the acting restaurant/customer identity.

Therefore ownership guards could not be evaluated.

REFUND_REQUESTED also had no payment/state eligibility guard.

## Hypothesis

If P07 guards are correct:
- another restaurant cannot accept an Order even though PLACED → ACCEPTED is a legal transition;
- another customer cannot cancel an Order even though PLACED → CANCELLED is legal;
- refund request fails when no payment was recorded;
- refund request fails when lifecycle is not REJECTED/CANCELLED;
- successful acceptance preserves identity/stable data, reaches ACCEPTED and appends exactly one acceptance occurrence;
- a guard failure through the application service leaves repository current state unchanged.

## Predictions

1. Wrong restaurant + PLACED + ACCEPT → OrderGuardViolationException.
2. Wrong customer + PLACED + CANCEL → OrderGuardViolationException.
3. Wrong restaurant + PLACED + START_PREPARATION → state error first, because state precondition fails before ownership guard.
4. CANCELLED without PAYMENT_RECORDED + refund request → refund-payment guard failure.
5. ACCEPTED after payment + refund request → refund-state guard failure.
6. Correct acceptance satisfies target-state and preservation postconditions.
7. Paid REJECTED Order can record REFUND_REQUESTED without changing REJECTED status.
8. Application-level guard failure does not replace repository current state.

## Setup

Executable tests:
- OrderGuardAndContractTest
- OrderWorkflowGuardIntegrationTest
- evolved OrderStateMachineTest
- evolved workflow tests.

## Controlled variables

Experiments vary:
- acting business identity;
- lifecycle state;
- presence/absence of PAYMENT_RECORDED.

They keep:
- one process;
- same repository;
- no authentication framework;
- no database;
- no concurrent writer;
- no real payment provider.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P07 CI.

## Limitations

The experiment does not prove:
- actor identity is authenticated;
- caller cannot forge IDs;
- guards are safe against stale authoritative state;
- refund actually executes;
- repeated commands are idempotent;
- business/technical failure mapping is final.

## Conclusion

Pending execution evidence.
