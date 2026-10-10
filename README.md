# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P14
Current evolution: C1.1-P15 candidate — Repeated Commands & Replay Semantics

## Part type

Type C — repeated-command / replay failure semantics.

## Verification

mvn -B -ntp verify

P15 is not frozen until the exact candidate passes CI.

## P15 question

When the same logical intent is submitted again:
- is it a retry or a new command?
- can the server know?
- is the business effect repeated?
- is the first result replayed?

## Current behavior

Sequential cancellation retry:
- final state converges to CANCELLED;
- retry is rejected as ILLEGAL_TRANSITION;
- original Accepted result is not replayed.

Concurrent identical cancellation:
- both requests can execute and return Accepted;
- current Order converges to one CANCELLED representation;
- this is not duplicate detection or at-most-once execution.

Repeated refund:
- second request is suppressed by REFUND_ALREADY_REQUESTED;
- original Accepted result is not replayed.

## Important boundary

P15 does not implement the later full idempotency mechanism.

Duplicate payment is reserved for P16.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no durable idempotency store;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
