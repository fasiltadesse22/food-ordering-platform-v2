# ADR-0019 — Freeze Cluster 1.1 Correctness Before Durable Persistence

Status: Accepted for C1.1-P19 candidate

## Context

Cluster 1.1 has evolved one real Spring Boot Food Ordering application through eighteen verified checkpoints.

The accumulated repository now contains executable evidence for:
- actors, use cases and responsibility boundaries;
- commands, decisions and domain facts;
- current Order identity and authority;
- workflow discovery;
- lifecycle state transitions;
- contextual guards and postconditions;
- terminality, reversibility and compensation;
- business rejection versus technical failure;
- validation, invariants and future database constraints;
- logical atomicity;
- evidence/claim strength;
- concurrency conflict windows;
- cancellation/acceptance races;
- repeated-command/replay semantics;
- duplicate payment and external effects;
- stale-state/temporal correctness;
- process-loss/durability non-guarantees.

The next cluster introduces entity/value-object/aggregate/consistency-boundary reasoning and real durable relational persistence.

## Decision

P19 is a synthesis/freeze part.

Do not change production architecture or add PostgreSQL.

Create a final Cluster 1.1 review package containing:
- final state-machine/workflow map;
- invariant/contract catalog;
- conflict/failure matrix;
- guarantee/non-guarantee matrix;
- diagnosis/design-defense package;
- Cluster 1.2 handoff;
- final evidence ledger and checkpoint manifest.

Re-run the complete root Maven verification and the P18 real process-restart experiment through CI.

The resulting verified commit becomes both:
- checkpoints/C1.1-P19;
- checkpoints/C1.1-FINAL.

## Why no new mechanism

P19 asks whether the current system can be explained, predicted, diagnosed and defended.

Adding persistence/versioning/idempotency or distributed mechanisms here would change the object being reviewed and erase the clean Cluster 1.1 before-state.

## Exit criterion

Cluster 1.1 is complete only when the current implementation can be defended with scoped claims:
- what is guaranteed;
- what is observed;
- what is structurally demonstrated;
- what remains assumed or hypothetical;
- what is explicitly not guaranteed;
- which later mechanism is now justified by which evidence.

## Handoff principle

Cluster 1.2 must inherit the C1.1-FINAL commit exactly.

It may replace the in-memory adapter only after preserving this baseline and must validate durable persistence/concurrency changes against the failure evidence accumulated here.
