# Checkpoint Manifest — C1.2-P04 Candidate

Part: 1.2.4 Deriving Aggregate Candidates from Invariants
Base: checkpoints/C1.2-P03 (55c6169194ebbc19aae437462ec1a7358a772f3d).
Evolution: Type A architecture derivation/ADR/evidence, no runtime/code/config/schema change.

Deliverables:
- architecture/scenarios/C1.2-P04-aggregate-candidate-derivation.md
- architecture/adr/ADR-0023-provisional-order-aggregate-candidate.md
- architecture/evidence/C1.2-P04-evidence.md
- this checkpoint manifest

Provisional decision: one Order-centered immediate consistency boundary and current Order as prospective root, based on established invariants/identity/atomicity evidence. Customer/Restaurant/provider full authority excluded. Status/history, owned lines, guard dependencies and relevant occurrences remain local to one Order's decisions.

Preserved fragilities: P13/P14 conflicting acknowledged decisions, P17 stale current overwrite, P18 process-loss, P15 repeated-command and P16 external-effect divergence.
Deferred: formal entity/value details P05/P06, root enforcement P07, Postgres P11/P12, SQL local transactions P15, version/conflict P16–P18, aggregate size P19/P20, cross authority P21.

Verification gate: exact-commit GitHub Actions must pass root mvn -B -ntp verify and existing real P18 process-restart script. Candidate document does not claim P04 CI has already passed.
Next: Part 1.2.5 Entity Identity, Equality, State and Lifecycle.
