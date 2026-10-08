# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P09
Current evolution: C1.1-P10 candidate — Business Invariants, Validation Rules and Database Constraints

## Verification

mvn -B -ntp verify

P10 is not frozen until GitHub Actions verifies the exact candidate.

## Rule layers

HTTP boundary validation:
- rejects malformed request shape early.

Domain structural validation:
- protects identifier and line value objects even outside HTTP.

Business invariants:
- Order state/history coherence is verified whenever an Order representation is constructed.

Future database constraints:
- documented for Cluster 1.2;
- not implemented in P10.

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
