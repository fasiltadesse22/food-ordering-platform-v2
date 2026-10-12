# Checkpoint Manifest — C1.2-P06 Candidate

Part: 1.2.6 Value Objects, Value Semantics and Immutability
Inherits: checkpoints/C1.2-P05 (2cc2277436b0729aed4e1712e7aa3cf9bad6521c)
Type A/C: six targeted executable tests and deep value/monetary/immutability architectural records. No production code/config/schema change.
Files: OrderValueSemanticsContractTest; P06 scenario, ADR-0025, experiment plan, evidence ledger, this manifest.
Prediction: numeric BigDecimal 5.0 vs 5.00 compares equal via compareTo but record equality false; current Order and DTO collections are defensively copied and immutable, and domain mutations yield new same-identity Orders.
Limitations: no Money/currency policy, normalization, persistence mapping, transaction, optimistic locking, distributed service or financial gateway correctness.
Preserve known P13/P14/P17/P18 failures.
Verification gate: GitHub Actions root mvn -B -ntp verify and real P18 restart script on exact candidate SHA before freezing checkpoints/C1.2-P06.
Next: 1.2.7 Aggregate Roots, Encapsulation and Invariant Enforcement.
