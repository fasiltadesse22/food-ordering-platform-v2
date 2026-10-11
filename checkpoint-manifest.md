# Checkpoint Manifest — C1.2-P02 Candidate

## Identity
Part: C1.2-P02 — Required State, Authority, Identity and Lifecycle Continuity
Inherits: `checkpoints/C1.2-P01` at `2a61909a2cf3470b3917caab2abecdafe9334e81`
Part type: A/C — source-grounded identity contract, deterministic continuity experiments; no production code/config/topology change.
Status of authored tree: CANDIDATE until exact-commit CI passes and checkpoint branch is created.

## Engineering question and prediction
What makes business state belong to the same continuing object through decisions, representations and process lifetimes?
Prediction: domain lifecycle methods preserve stable OrderId across new object instances; distinct identities remain independent;
a correct identifier doesn't imply current shared authority in another repository or JVM.

## Changes
New `OrderIdentityContinuityContractTest` with 5 tests for lifecycle identity, distinct IDs, typed value equality,
repository authority scope, and stale correct-ID representations.
New detailed scenario, provisional ADR-0021 identity-continuity decision, experiment plan, evidence ledger and this manifest.
Unchanged: production Java, HTTP, Maven dependencies, CI workflow, in-memory authority, current domain invariants,
known lost-update/stale-write/process-loss/duplicate external payment failures.

## Verification requirement
GitHub Actions must execute on the exact resulting P02 commit:
`mvn -B -ntp verify`
`experiments/cluster-1.1/p18-process-restart-durability.sh`
When successful, create `checkpoints/C1.2-P02` at precisely the verified commit.
P01's successful run 38038951502 is inherited evidence and must not be counted as P02 verification.

## Guarantees, limits, deferred work
Structurally: typed OrderId, immutable normal lifecycle successors, per-repository value-equal lookup, no version.
Tests targeted: identity survives evolution; independent IDs; equality semantics; independent repository authority;
stale same-ID state does not become authoritative without a save.
Not guaranteed: global UUID uniqueness, durable reconstructability, transaction semantics,
concurrency serialization, freshness, provider payment truth, security authentication.

Deferred: aggregate formalization P04/P07, persistence P11/P12, transaction P15, isolation P16,
optimistic version P17, service split and distributed mechanisms.

## Handoff
Next part P03 — Atomicity Requirements and the Shape of a Consistency Boundary.
Do not change core domain topology before P03/P04 identity/consistency analysis.
