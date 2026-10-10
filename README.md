# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P14 — Cancellation / Acceptance Race
Previous checkpoint: checkpoints/C1.1-P13

## Part type

Type C — cross-actor concurrency failure evolution.

## Verification

GitHub Actions run:
38027340494

Command:
mvn -B -ntp verify

Observed:
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified P14 conflict

From PLACED:

Customer CANCEL:
PLACED → CANCELLED

Restaurant ACCEPT:
PLACED → ACCEPTED

Serial execution:
only the first authoritative transition succeeds.

Controlled shared-snapshot concurrency:
both actors can receive Accepted;
last save determines current authority;
reversing only save order reverses the surviving outcome.

## Critical distinction

Concurrency mechanism:
How do we ensure one coherent winner?

Business policy:
Which actor should win under which business conditions?

The current implementation answers neither correctly by design.

It merely exhibits accidental last-write-wins timing.

## Business policy remains open

Candidate alternatives:
- first authoritative commit wins;
- cancellation priority before a defined cutoff;
- acceptance priority after fulfillment commitment.

No policy is implemented in P14.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no locking/versioning;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
