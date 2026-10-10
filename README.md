# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P13 — Conflicting Operations and Concurrency Windows
Previous checkpoint: checkpoints/C1.1-P12

## Part type

Type C — controlled concurrency failure evolution.

## Verification

GitHub Actions run:
38026585059

Command:
mvn -B -ntp verify

Observed:
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified concurrency failure

Sequential fresh-state behavior:
- ACCEPT from PLACED succeeds;
- REJECT then observes ACCEPTED and is rejected.

Controlled concurrency:
- ACCEPT and REJECT can both capture the same PLACED Order;
- both can return Accepted;
- last save becomes current authority;
- reversing only save order reverses the surviving decision.

## Root mechanism

Current application workflow:

find current Order
→ decide against returned immutable snapshot
→ save complete successor

No expected-version comparison protects the interval between read and save.

ConcurrentHashMap protects individual map operations, not the full business transaction.

## Important scope

P13 proves selected deterministic interleavings.

It does not measure race frequency under real traffic and does not establish database/multi-JVM behavior.

No fix is added yet.

P14 will apply the same mechanism to customer cancellation versus restaurant acceptance.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no locking/versioning;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
