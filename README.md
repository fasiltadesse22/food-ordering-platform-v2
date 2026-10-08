# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P10 — Business Invariants, Validation Rules and Database Constraints
Previous checkpoint: checkpoints/C1.1-P09

## Verification

mvn -B -ntp verify

P10 implementation verification:

Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Rule layers

HTTP boundary validation:
- rejects malformed request shape early.

Domain structural validation:
- protects identifier and line value objects even outside HTTP.

Business invariants:
- Order state/history coherence is verified whenever an Order representation is constructed.

Future database constraints:
- documented for Cluster 1.2;
- not implemented or claimed in P10.

## Important distinction

Passing validation does not prove business invariant correctness.

A database constraint can reinforce a rule without becoming the business rule itself.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory repository;
- no PostgreSQL;
- no transaction manager;
- no messaging.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
