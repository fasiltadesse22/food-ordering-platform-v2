# Food Ordering Platform — V8.0 Fresh-Start Evolution

This repository is the evolving project for Enterprise Distributed Systems Architecture & System Design — V8.0.

Current verified learning checkpoint:

C1.1-P03
Intent, Commands, Decisions, Facts and Events

Previous preserved checkpoint:

checkpoints/C1.1-P02

## Verification

mvn -B -ntp verify

P03 implementation verification:

Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Current semantic flow

Customer intent
→ PlaceOrderRequest
→ PlaceOrderCommand
→ application decision
→ Accepted or Rejected result
→ OrderPlaced local domain fact only on accepted path

## Important event/fact boundary

OrderPlaced is a local semantic domain fact.

It is not:
- a Kafka record;
- an integration event contract;
- an outbox row;
- an event-store record;
- proof of event-driven architecture.

## Current topology

- one Java 21 / Spring Boot deployable;
- framework-independent domain/application core;
- in-memory persistence adapter;
- minimal HTTP adapter;
- no distributed communication after the HTTP boundary.

## Deliberately absent

- PostgreSQL
- Kafka
- Redis
- Saga
- Outbox
- CQRS
- Event Sourcing
- distributed locks
- service mesh
- Kubernetes
- database-per-service
- premature service decomposition

See checkpoint-manifest.md, architecture/scenarios, architecture/adr and architecture/evidence for the evidence-qualified model.
