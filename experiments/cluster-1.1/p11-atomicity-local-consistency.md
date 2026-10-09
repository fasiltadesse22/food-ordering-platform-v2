# C1.1-P11 Experiment — Logical Atomicity and Local Consistency

## Question

Which related changes must be treated as one logical decision, and what happens if a failure occurs between independently committed representations of that decision?

## Execution

GitHub Actions run: 37886764685

Command:

mvn -B -ntp verify

## Observation

OrderAtomicityAndConsistencyTest:
- tests run: 4
- failures: 0
- errors: 0
- skipped: 0

OrderLocalCommitBoundaryTest:
- tests run: 2
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 68
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed:
- normal completion constructs one COMPLETED Order containing ORDER_COMPLETED;
- the previous PREPARING Order remains unchanged because Order is immutable;
- status-first split write followed by injected failure leaves COMPLETED without ORDER_COMPLETED and violates the invariant;
- history-first split write followed by injected failure leaves PREPARING with ORDER_COMPLETED and violates the invariant;
- completing both split writes restores state/history consistency;
- repository failure before authority replacement leaves old PREPARING authority unchanged;
- successful repository replacement exposes a complete COMPLETED representation;
- all inherited P01-P10 tests remain green.

## Interpretation

Lifecycle state and its explaining workflow occurrence belong to one logical completion decision in the current model.

Splitting them into independently committed authorities creates failure windows in both write orderings.

The current immutable Order + single current-reference replacement avoids that specific partial-publication problem locally.

This does not prove a database transaction, durability, or atomic concurrent read-modify-write.

## Limitations

Not proven:
- real database commit/rollback;
- process-crash durability;
- multiple aggregate atomicity;
- concurrent conflict protection;
- remote participant atomicity;
- response-loss idempotency.

## Conclusion

P11 establishes the logical atomicity requirement and demonstrates the failure window that future transaction mechanisms must close.
