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

## P19 verification

Candidate commit:
a2f1360dc0a52327300b561fa3900bae60ae1f6f

GitHub Actions:
38035664742

Root Maven observation:
- tests run: 86;
- failures: 0;
- errors: 0;
- skipped: 0;
- BUILD SUCCESS.

P18 runtime observation reproduced inside the P19 candidate:
- process A started;
- Order 09eeaf7e-555e-453a-9765-541f7a50ad02 was CANCELLED and returned HTTP 200;
- process A PID 2481 terminated with SIGKILL;
- process B started from the same application artifact;
- GET same OrderId returned HTTP 404.

## Cluster exit conclusions

Executed and verified:
1. all current Maven tests remain green;
2. synthesis artifacts do not alter production behavior;
3. the intentionally non-durable P18 baseline remains reproducible.

Structurally audited:
- one deployable remains;
- in-memory Order authority remains;
- no PostgreSQL/version/idempotency/distributed mechanism has been introduced.

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

Cluster exit behavior gate PASSED for the P19 candidate.

The evidence-bearing commit must still pass the same CI gates before checkpoint branches are created.
