# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P16 — Duplicate Payment & External-Effect Thinking
Previous checkpoint: checkpoints/C1.1-P15

## Part type

Type C — duplicate-payment / external-effect failure evolution.

## Verification

GitHub Actions run:
38032714055

Command:
mvn -B -ntp verify

Observed:
Tests run: 82, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified P16 findings

Repeated local payment:
- both calls Accepted;
- current Order contains two PAYMENT_RECORDED facts.

Repeated test-only charge→record:
- external charges = 2;
- local facts = 2.

Charge commits, provider response lost, then retry:
- external charges = 2;
- local facts = 1.

Local record first, provider fails before charge:
- external charges = 0;
- local facts = 1.

## Critical rule

PAYMENT_RECORDED is local Order history.

It is not proof that an external provider:
- charged exactly once;
- charged at all;
- returned a response;
- shares one atomic commit with Order state.

## Architectural conclusion

Changing call order between two independently committed authorities moves the inconsistency window.

It does not remove it.

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
