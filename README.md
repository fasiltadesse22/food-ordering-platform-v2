# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P13
Current evolution: C1.1-P14 candidate — Cancellation / Acceptance Race

## Part type

Type C — cross-actor concurrency failure evolution.

## Verification

mvn -B -ntp verify

P14 is not frozen until the exact candidate passes CI.

## P14 conflict

From PLACED:

Customer CANCEL:
PLACED → CANCELLED

Restaurant ACCEPT:
PLACED → ACCEPTED

Sequentially, whichever transition becomes authoritative first makes the other illegal.

Under the preserved shared-snapshot race, both can derive a locally valid successor before either observes the other's save.

## Critical distinction

Concurrency mechanism:
How do we ensure one coherent winner?

Business policy:
Which actor should win under which business conditions?

P14 does not silently answer the second question with implementation timing.

## Current accidental behavior

Unconditional last-write-wins.

This is not approved business policy.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no locking/versioning;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
