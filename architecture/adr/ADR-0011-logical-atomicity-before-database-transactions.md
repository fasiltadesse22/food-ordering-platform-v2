# ADR-0011 — Make Logical Atomicity Explicit Before Database Transactions

Status: Accepted for C1.1-P11 candidate

## Context

P10 established an invariant between lifecycle state and workflow history.

Example:

COMPLETED
requires
RESTAURANT_ACCEPTED
+ PREPARATION_STARTED
+ ORDER_COMPLETED

The current Order implementation already creates a new immutable Order containing both:
- the next lifecycle state; and
- the workflow occurrence that explains it.

The in-memory repository then replaces one map entry with that complete Order.

This useful property must not be confused with:
- a database transaction;
- crash durability;
- read-modify-write concurrency control;
- multi-aggregate atomicity;
- distributed atomicity.

## Decision

Make the local mutation unit explicit with OrderEvolution:

OrderEvolution(
  nextStatus,
  occurrence
)

Order.transition builds this pair and Order.apply constructs one new invariant-checked immutable Order.

The normal production path therefore never intentionally publishes a lifecycle target state separately from its explaining workflow occurrence.

## Controlled fragile alternative

Preserve an experiment that stores:
- lifecycle status; and
- workflow history

as independent mutable authorities.

For completion, two write orderings are tested.

### State first

PREPARING/history valid
→ write status = COMPLETED
→ injected failure
→ ORDER_COMPLETED not written

Observed combined state is invariant-invalid.

### History first

PREPARING/history valid
→ append ORDER_COMPLETED
→ injected failure
→ status remains PREPARING

Observed combined state is invariant-invalid.

Only after both writes complete does the split representation become consistent again.

## Local repository publication

InMemoryOrderRepository.saveCurrent performs one ConcurrentHashMap put of one immutable Order reference.

For the current single-process model this means the repository exposes either:
- the previous complete Order; or
- the new complete Order

for that map entry after the replacement.

This does NOT make the full use case atomic under concurrency.

findCurrentById
→ domain evolution
→ saveCurrent

is still a compound read-modify-write sequence.

Two callers can race. That is intentionally deferred.

## Failure before authority replacement

A deterministic repository experiment proves:

- domain can construct a valid COMPLETED Order in local memory;
- repository save can fail before replacement;
- authoritative current state remains PREPARING.

Therefore:
valid local evolution != committed authoritative outcome.

## What atomicity means here

Logical atomicity means the changes required by one business decision should become visible together or not at all within the relevant consistency boundary.

It does not mean:
- fast;
- one CPU instruction;
- @Transactional annotation;
- distributed transaction;
- durability.

## Why no PostgreSQL / @Transactional

The engineering question is to discover the atomicity requirement before choosing the persistence mechanism.

Cluster 1.2 will introduce PostgreSQL and real transaction behavior.

Adding @Transactional now would teach annotation-first reasoning and would not provide any real database transaction anyway.

## Preserved fragilities

- find + evolve + save is not atomic read-modify-write;
- no optimistic version check;
- no persistent transaction;
- no durability;
- no multi-Order atomicity;
- no cross-participant atomicity;
- response can still be lost after authority replacement;
- paid-but-PLACED modification and payment-after-cancellation remain unresolved.

## Evolution

P12 will classify what has actually been observed versus inferred or guaranteed.

P13+ will expose concurrent conflicting operations.

Cluster 1.2 will later test real PostgreSQL transaction boundaries against the atomicity requirements discovered here.
