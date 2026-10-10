# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P18
Current evolution: C1.1-P19 / C1.1-FINAL candidate

## P19

Correctness Synthesis, Diagnosis, Design Defense and Cluster Freeze.

Engineering question:

Can we prove, explain, diagnose and defend what the current system guarantees—and what it does not?

## Project evolution

Type A only.

Production behavior remains unchanged.

P19 adds the final Cluster 1.1 review package under:
architecture/cluster-1.1/

## Verification

CI continues to execute:
- mvn -B -ntp verify
- experiments/cluster-1.1/p18-process-restart-durability.sh

The cluster is not frozen until the exact P19 candidate and evidence-bearing commit pass those gates.

## Intended final baseline

One real Java 21 / Spring Boot deployable.

Current in-process InMemoryOrderRepository authority.

Known/preserved failure evidence for:
- concurrency;
- replay;
- external payment effects;
- stale writes;
- restart durability.

No PostgreSQL or later distributed mechanisms yet.

## Next

Cluster 1.2 — Entities, Value Objects, Aggregates & Consistency Boundaries.

It must inherit C1.1-FINAL exactly and evolve durable relational persistence/consistency mechanisms only from the evidence preserved here.
