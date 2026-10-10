# Checkpoint Manifest — C1.2-P01 Candidate

## Identity
- Part: C1.2-P01
- Cluster: 1.2 — Entities, Value Objects, Aggregates & Consistency Boundaries
- Title: Inherited Correctness Pressure and Consistency-Boundary Discovery
- Evolution: Type A — analysis/evidence documentation only
- Inherited frozen branch: `checkpoints/C1.1-FINAL`
- Inherited frozen commit: `d76cfdf585e19afeef21b9d522351541c5cbc07f`
- Status of this authored tree: CANDIDATE; must not be called VERIFIED/FROZEN until CI succeeds for exact resulting commit and checkpoint branch is created.

## Engineering question
Which existing correctness requirements demand a shared consistency boundary?
Prediction: selected lifecycle status/history invariants imply one coherent local Order decision boundary,
while independent Customer, Restaurant and external payment truth do not enter that boundary merely by reference.

## Changes justified
- Scenario/state-dependency map in `architecture/scenarios/C1.2-P01-consistency-boundary-discovery.md`.
- Provisional ADR-0020 under `architecture/adr/`.
- Inherited-experiment plan under `experiments/cluster-1.2/`.
- P01 evidence ledger under `architecture/evidence/`.
- This manifest.
No production Java, Maven config, database, tests, CI, networking, or application topology changes.

## Verification gate
Existing GitHub workflow must run for exact candidate commit:
`mvn -B -ntp verify`
`experiments/cluster-1.1/p18-process-restart-durability.sh`
The C1.1-FINAL baseline was verified by workflow run 38035876593; **that cannot substitute for P01's own commit verification**.
P01 candidate CI result is intentionally left to the GitHub Actions record (no invented run ID or PASS assertion).
Freeze `checkpoints/C1.2-P01` only after both required steps are green on the exact commit.
The action run linked to the commit and the checkpoint branch SHA are the final external verification record.

## Preserved known fragilities
- Concurrent mutually exclusive decisions from P13/P14;
- stale replacement and state resurrection from P17;
- in-memory process loss from P18;
- repeated command and external payment effect issues from P15/P16.
No mitigation is introduced or claimed.

## Evidence language
Observed (inherited): specified CI execution and recorded failure experiments.
Structurally demonstrated: selected invariant checks, immutable successors, unconditional in-memory save.
Inferred: Order-centered immediate consistency boundary is plausible.
Not guaranteed: durable authority, conflict detection, comprehensive history correctness, cross-authority atomicity or idempotency.

## ADR and deferred mechanisms
ADR-0020 records provisional candidate only. PostgreSQL, schema migrations, DB constraints,
database transactions, optimistic/pessimistic locks, Kafka, Redis, Saga, Outbox, Inbox, CQRS,
Event Sourcing, distributed locks, service split and Cloud/Kubernetes deployment remain deferred.

## Next part
C1.2-P02 — Required State, Authority, Identity and Lifecycle Continuity.
