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

## Execution

GitHub Actions run: 37753011313

Command:

mvn -B -ntp verify

## Observation

OrderGuardAndContractTest:
- tests run: 7
- failures: 0
- errors: 0
- skipped: 0

OrderWorkflowGuardIntegrationTest:
- tests run: 1
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 36
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed:
- wrong restaurant + PLACED + ACCEPT was rejected by RESTAURANT_DOES_NOT_OWN_ORDER;
- wrong customer + PLACED + CANCEL was rejected by CUSTOMER_DOES_NOT_OWN_ORDER;
- wrong restaurant + PLACED + START_PREPARATION failed the state precondition first;
- CANCELLED without PAYMENT_RECORDED could not request refund;
- ACCEPTED after payment could not request refund;
- successful acceptance preserved identity, ownership, order content, placedAt and total, changed state to ACCEPTED and appended exactly one acceptance occurrence;
- paid REJECTED Order could request refund without changing REJECTED lifecycle state;
- application-level restaurant guard failure left the exact same repository Order instance authoritative.

## Interpretation

State-source legality is necessary but not sufficient.

Contextual business facts must also satisfy the operation contract.

Successful operations have observable postconditions; failed guards preserve current state.

## Limitations

The experiment does not prove:
- actor identity is authenticated;
- caller cannot forge IDs;
- guards are safe against stale authoritative state;
- refund actually executes;
- repeated commands are idempotent;
- business/technical failure mapping is final.

## Conclusion

P07 successfully adds contextual guard and contract enforcement without introducing security, persistence or concurrency mechanisms.
