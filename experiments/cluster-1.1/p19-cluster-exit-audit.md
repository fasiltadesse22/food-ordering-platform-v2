# C1.1-P19 — Cluster Exit Audit

## Engineering question

Can we prove, explain, diagnose and defend what the current system guarantees—and what it does not?

## Project evolution type

Type A — synthesis/freeze only.

No production code/config mechanism is added by P19.

## Repository audit

The inherited repository contains:
- ADR-0001 through ADR-0018;
- evidence records for P01 through P18;
- scenario/contract artifacts for each conceptual progression;
- executable Maven tests covering lifecycle, guards, invariants, failures, races, replay, payment effects and stale state;
- the CI-executed P18 two-process durability experiment.

P19 adds:
- ADR-0019;
- final state-machine/workflow map;
- final invariant/contract catalog;
- final conflict/failure matrix;
- final guarantee/non-guarantee matrix;
- diagnosis/design-defense package;
- Cluster 1.2 handoff;
- final evidence ledger/manifest.

## Cluster exit predictions

The exact P19 candidate must:
1. pass the complete Maven reactor;
2. retain all P01-P18 behavior;
3. rerun the real P18 JVM replacement experiment;
4. continue to demonstrate the intentionally non-durable baseline;
5. add no production PostgreSQL/versioning/idempotency/distributed mechanism.

## Diagnosis mastery

The learner must independently diagnose at least these failure families:
- illegal transition/guard rejection;
- technical repository failure;
- concurrent conflicting decisions;
- duplicate/replayed command;
- unknown external payment outcome;
- stale-state overwrite;
- process-loss durability failure.

## Design-defense mastery

The learner must be able to defend:
- one deployable remains simpler and sufficient for current learning forces;
- local immutable evolution protects representation atomicity but not concurrent history;
- in-memory authority was useful but is now insufficient for durability;
- PostgreSQL is now justified by evidence;
- Kafka/Saga/Outbox/distributed locks are still premature.

## Communication mastery

Explain the same platform for:
- backend engineer: mechanics and code boundaries;
- Staff/Principal engineer: forces, authority, invariants, failure windows and trade-offs;
- engineering manager: business risk, sequencing and why complexity is deferred;
- nontechnical stakeholder: which promises the system can/cannot safely make.

## Transfer

Apply the same reasoning to an unfamiliar domain:
reservation, funds transfer, inventory allocation, workflow approval or resource provisioning.

## Exit status

Pending exact P19 CI and evidence-bearing re-verification.
