# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P03
Current evolution: C1.1-P04 candidate — Identity and Authoritative State

## Verification

mvn -B -ntp verify

P04 is not frozen until GitHub Actions verifies the exact candidate.

## Current identity/authority model

- OrderId is business identity for Order.
- Java object identity is not business identity.
- application use cases operate on typed OrderId rather than transport strings;
- OrderRepository is current in-process Order-state authority;
- OrderSnapshot is a detached observation and may become stale;
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
