# ADR-0018 — Current In-Process Authority Is Not a Durability Guarantee

Status: Accepted for C1.1-P18 candidate

## Context

P04 established the repository as current Order authority within the running application.
P17 established that authority also needs freshness/conflict semantics.

P18 asks a different question:

What happens to correctness claims when the process that owns the current authority disappears?

The current adapter stores Orders only in a ConcurrentHashMap owned by one JVM.

That map can be authoritative while the process is alive and still provide no durability across process loss.

## Decision

Preserve InMemoryOrderRepository.

Do not introduce PostgreSQL or another durable store in P18.

Add a CI-executed runtime experiment that:
1. builds the real Spring Boot jar;
2. launches process A;
3. places and cancels an Order through real HTTP;
4. proves process A returns that Order as CANCELLED;
5. kills process A with SIGKILL;
6. launches process B from the same artifact;
7. queries the same OrderId;
8. requires HTTP 404.

This is stronger evidence than constructing a second repository object in a unit test because it crosses a real JVM process boundary.

## Required distinction

authoritative now != durable.

saveCurrent returned != state survives process death.

successful HTTP response != durable commit.

ConcurrentHashMap thread safety != persistence.

new process availability != business-state recovery.

restart recovery != disaster recovery.

## Why SIGKILL

SIGKILL intentionally prevents application-level graceful shutdown logic from running.

The experiment asks whether current Order state survives process loss because it is durably persisted.

It does not test graceful shutdown hooks.

## Why no database

Cluster 1.2 is responsible for introducing real durable relational persistence and then validating transaction/constraint/concurrency behavior.

Adding PostgreSQL in P18 would erase the exact fragile before-state whose failure is needed to justify that evolution.

## Evidence limits

If process B returns 404, P18 may claim only:
- the selected Order existed in process A;
- process A was killed;
- a new JVM process using the same current application/artifact did not recover that Order;
- the current in-memory adapter provides no demonstrated restart durability.

P18 does not prove:
- all process crashes always lose every byte in all possible systems;
- how a database would behave;
- crash consistency of filesystem/database writes;
- replication, backup, RPO/RTO, HA or DR behavior.

## Forward pressure

Cluster 1.2 must now treat durable persistence as an evidence-backed correctness requirement rather than a technology preference.
