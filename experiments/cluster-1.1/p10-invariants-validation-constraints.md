# C1.1-P10 Experiment — Invariants, Validation and Constraints

## Question

Can the project demonstrate that structurally valid data may still violate a business invariant, while HTTP validation and future database constraints remain separate enforcement layers?

## Execution

GitHub Actions run: 37791411073

Command:

mvn -B -ntp verify

## Observation

OrderInvariantAndValidationTest:
- tests run: 6
- failures: 0
- errors: 0
- skipped: 0

OrderHttpIntegrationTest:
- tests run: 8
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 62
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed:
- blank CustomerId and RestaurantId fail structural domain validation;
- zero quantity and negative unit price fail structural domain validation;
- empty Order lines fail HTTP Bean Validation before use-case/domain construction;
- blank HTTP customerId fails boundary validation;
- non-positive HTTP line quantity fails boundary validation;
- blank cancellation customer context fails boundary validation before business evaluation;
- structurally valid enum/fact/time values can still form an invariant-invalid PREPARING + ORDER_CANCELLED combination;
- COMPLETED without preparation history is rejected by OrderInvariants;
- normal public CANCELLED -> PREPARING attempt is rejected by the state-machine rule before an invariant-invalid Order can be created;
- PAYMENT_RECORDED + ORDER_CANCELLED + REFUND_REQUESTED remains invariant-valid while OrderStatus stays CANCELLED;
- all inherited P01-P09 behavior remains green.

## Interpretation

Validation and invariant enforcement answer different questions.

Passing HTTP/type validation proves only that values are structurally acceptable.

OrderInvariants protects semantic consistency among lifecycle state and workflow history.

No database exists, so no persistence constraint has been experimentally verified.

## Limitations

This experiment does not prove:
- PostgreSQL constraints;
- durable invariant enforcement;
- transaction atomicity;
- concurrent invariant preservation;
- stale-state correctness.

## Conclusion

P10 successfully separates boundary/domain validation from business invariants while preserving database-constraint reasoning as a future Cluster 1.2 mechanism.
