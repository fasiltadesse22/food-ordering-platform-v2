# Checkpoint Manifest — C1.2-P05 candidate

Part: 1.2.5 Entity Identity, Equality, State and Lifecycle
Inherits: checkpoints/C1.2-P04 (0c3efa497887930618e6ab9dc3b7503b09c5ceb8)
Evolution: Type A/C — source-grounded equality ADR, 5 targeted tests, no production Java or persistence changes.
Changes: new OrderEntityIdentityEqualityTest, P05 scenario, ADR-0024, experiment plan, evidence ledger, this manifest.
Preserved fragilities: P13/P14 concurrent invalid acknowledgements; P17 stale overwrite; P18 process-loss; P15 replay; P16 provider divergence.
Deferred: P06 deep Value Object semantics, P07 root encapsulation, P11–14 Postgres/persistence and ORM, P15 transactions, P16–18 conflict/versioning.
Verification gate: root Maven verify plus P18 real-process negative control on exact evidence-bearing commit; do not freeze until successful CI.
Next: C1.2-P06 Value Objects, Value Semantics and Immutability.
