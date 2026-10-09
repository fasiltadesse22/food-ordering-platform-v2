# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P11 — Atomicity and Local Consistency Requirements
Previous checkpoint: checkpoints/C1.1-P10

## Verification

mvn -B -ntp verify

P11 implementation verification:

Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Local atomicity model

OrderEvolution binds:
- next lifecycle state;
- explaining workflow occurrence.

One immutable invariant-checked Order is constructed before one repository replacement publishes it as current in-process authority.

## Verified failure experiment

If status and workflow history are split into independent writes:
- state-first failure produces invalid partial state;
- history-first failure produces invalid partial state.

Both related changes therefore belong to one logical completion decision.

## Important non-guarantees

This is NOT:
- a database transaction;
- durability;
- atomic read-modify-write under concurrency;
- multi-Order atomicity;
- distributed atomicity.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory repository;
- no PostgreSQL;
- no @Transactional;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
