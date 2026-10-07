# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P04 — Identity and Authoritative State
Previous checkpoint: checkpoints/C1.1-P03

## Verification

mvn -B -ntp verify

P04 implementation verification:

Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Current identity/authority model

- OrderId is business identity for Order.
- Java object identity is not business identity.
- application use cases operate on typed OrderId rather than transport strings;
- OrderRepository is current in-process Order-state authority;
- OrderSnapshot is a detached observation and can become stale;
- OrderResponse is transport representation, not authority;
- OrderPlaced is a business fact, not current-state storage.

## Current topology

- one Java 21 / Spring Boot deployable;
- in-memory current-state repository;
- no database;
- no optimistic locking;
- no messaging;
- no service decomposition.

See checkpoint-manifest.md, architecture/adr, architecture/scenarios and architecture/evidence for the evidence-qualified model.
