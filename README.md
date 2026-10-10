# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P15 — Repeated Commands & Replay Semantics
Previous checkpoint: checkpoints/C1.1-P14

## Part type

Type C — repeated-command / replay failure semantics.

## Verification

GitHub Actions run:
38032248389

Command:
mvn -B -ntp verify

Observed:
Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified replay behavior

Sequential cancellation retry:
- first call Accepted(CANCELLED);
- retry ILLEGAL_TRANSITION;
- final state contains one cancellation effect;
- original result is not replayed.

Concurrent identical cancellation:
- both calls can execute and return Accepted;
- two saves occur;
- current Order still converges to one CANCELLED representation.

Therefore:
same final state does not prove duplicate detection or at-most-once execution.

Repeated refund:
- second request is suppressed by REFUND_ALREADY_REQUESTED;
- original Accepted result is not replayed.

## Current replay architecture

No logical command ID/idempotency key exists.

The server cannot explicitly distinguish:
- retry of one logical command;
- a new same-looking command.

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
