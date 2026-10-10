# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P17
Current evolution: C1.1-P18 candidate — Process Crash, Transient State & Durability Non-Guarantees

## Part type

Type C — process-loss / durability failure evolution.

## Verification

Root:
mvn -B -ntp verify

Runtime durability experiment:
experiments/cluster-1.1/p18-process-restart-durability.sh

P18 is not frozen until both checks execute successfully in CI.

## P18 question

What remains of acknowledged Order state after the JVM that owns the current in-memory authority disappears?

## Current authority

InMemoryOrderRepository:
- authoritative inside the running application;
- ConcurrentHashMap-backed;
- no durable recovery source.

## Controlled experiment

Process A:
place → cancel → GET = CANCELLED.

SIGKILL process A.

Process B:
start same jar → GET same OrderId.

Expected:
404.

## Critical distinctions

authoritative != durable.

successful response != durable commit.

restart of compute != recovery of business state.

## Important boundary

P18 proves the durability failure before fixing it.

PostgreSQL remains deferred to Cluster 1.2.

P19 next synthesizes and freezes Cluster 1.1.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
