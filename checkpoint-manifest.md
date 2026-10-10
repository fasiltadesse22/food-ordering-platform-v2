# Checkpoint Manifest — C1.1-P19 / C1.1-FINAL

## Identity

- Part checkpoint: C1.1-P19
- Cluster checkpoint: C1.1-FINAL
- Status: VERIFIED — FREEZE PENDING EXACT EVIDENCE-COMMIT CI
- Inherited checkpoint: C1.1-P18
- Inherited branch: checkpoints/C1.1-P18
- Inherited commit: 8644f76bcf77e23941f4747c3527a950a6352762
- P19 candidate commit: a2f1360dc0a52327300b561fa3900bae60ae1f6f
- P19 candidate verification: 38035664742

## Part title

Correctness Synthesis, Diagnosis, Design Defense and Cluster Freeze.

## Engineering question

Can we prove, explain, diagnose and defend what the current system guarantees—and what it does not?

## Project evolution type

Type A — synthesis/freeze.

## Production code/config

Unchanged.

No new production mechanism is introduced.

## Verified candidate gate

mvn -B -ntp verify

Observed:
Tests run: 86
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

P18 real process experiment also reran:
- CANCELLED / HTTP 200 before SIGKILL;
- same OrderId / HTTP 404 after fresh JVM startup.

## Final outputs

- final state-machine/workflow map;
- invariant/contract catalog;
- conflict/failure matrix;
- guarantee/non-guarantee matrix;
- evidence ledger;
- ADR-0019;
- diagnosis/design-defense package;
- Cluster 1.2 handoff;
- P19 exit audit.

## Final unresolved policies deliberately preserved

- definitive payment sequencing;
- payment authorization versus capture;
- payment-after-cancellation policy;
- modification-after-payment policy;
- cancellation semantics after acceptance/preparation;
- real refund completion semantics;
- real payment-provider identity/idempotency/reconciliation.

## Mechanisms now evidence-justified for later evaluation

- durable relational Order persistence;
- one local transaction around aggregate-consistent state;
- selected relational constraints;
- authority-level stale/concurrent write detection, likely via version-aware persistence depending on Cluster 1.2 aggregate reasoning.

## Mechanisms still not justified here

- microservice split;
- Kafka;
- Redis;
- Saga;
- Outbox;
- CQRS;
- Event Sourcing;
- distributed locks;
- database-per-service;
- Kubernetes/AWS runtime architecture.

## Freeze gate remaining

The evidence-bearing commit containing this manifest/evidence must:
1. pass root Maven verification;
2. reproduce the P18 process-loss experiment;
3. then be branched as both checkpoints/C1.1-P19 and checkpoints/C1.1-FINAL.

## Next enabled cluster

Cluster 1.2 — Entities, Value Objects, Aggregates & Consistency Boundaries.
