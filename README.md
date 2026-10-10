# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P17 — Stale-State Decisions & Temporal Correctness
Previous checkpoint: checkpoints/C1.1-P16

## Part type

Type C — controlled stale-state / temporal-correctness failure evolution.

## Verification

GitHub Actions run:
38034398881

Command:
mvn -B -ntp verify

Observed:
Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified P17 findings

Stale modification after payment:
- newer PAYMENT_RECORDED can be erased.

Stale payment after cancellation:
- newer cancellation can be erased;
- lifecycle can be resurrected from CANCELLED to PLACED.

Fresh payment after cancellation:
- currently allowed by the payment milestone semantics;
- remains CANCELLED and retains both workflow facts;
- this is a separate policy gap, not stale corruption.

Fresh vs stale modification:
- current ACCEPTED Order correctly rejects modification;
- stale PLACED copy passes the same guard;
- stale save can erase acceptance.

## Critical rule

A guard being correct for the object it evaluates does not prove the object is still authoritative.

Immutability does not imply freshness.

## Important boundary

No versioning, locking or PostgreSQL mechanism is added in P17.

P18 next studies process crash/transient-state/durability non-guarantees.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no version field;
- no expected-version save;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
