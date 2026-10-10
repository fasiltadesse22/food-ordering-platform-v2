# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P15
Current evolution: C1.1-P16 candidate — Duplicate Payment & External-Effect Thinking

## Part type

Type C — duplicate-payment / external-effect failure evolution.

## Verification

mvn -B -ntp verify

P16 is not frozen until the exact candidate passes CI.

## Critical distinction

PAYMENT_RECORDED is a local Order workflow milestone.

It is not proof that an external payment authority:
- charged exactly once;
- charged at all;
- returned a response;
- can be atomically committed with Order state.

## P16 controlled external-effect harness

Test-only PaymentAuthority models an independently committed charge.

It is not a production provider integration.

Expected failure windows:
- duplicate charge and duplicate local fact;
- charge commits but response is lost, retry charges again;
- local PAYMENT_RECORDED exists while provider failed before charge.

## Architectural conclusion under test

Ordering two independently committed operations does not make them atomic.

Changing:
external → local

to:
local → external

moves the inconsistency window instead of eliminating it.

## Important boundary

No idempotency mechanism, Saga, Kafka or Outbox is introduced in P16.

P17 next studies stale-state decisions.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no real payment provider adapter;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
