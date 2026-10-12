# ADR-0028 — Protect Initial Placement Fact Semantics, Defer Integration Event Infrastructure

Status: Accepted for C1.2-P09.
Base: checkpoints/C1.2-P08, commit 5f78c68fc04b76f34fbc431010a677c0ba83bc9d.

## Context
OrderPlaced.from(Order) derives a historical placement fact by reading current Order.id/customerId/restaurantId/placedAt/total, regardless of whether the supplied representation is the initial placement state.
After modifyLines or recordPayment, Order may remain PLACED with new history and possibly changed total; after acceptance, status can differ. Reconstructing historical OrderPlaced from these later snapshots conflates historical fact and current state.
PlaceOrderService currently constructs fact after repository.saveCurrent; existing in-process repository is non-durable with no broker.
P16 payment evidence proves local facts are not externally authoritative; P18 proves process-lifetime history is not durable.

## Decision
Add a narrow precondition to OrderPlaced.from(Order): supplied Order must be initial PLACED representation and contain no workflow occurrences. Raise IllegalArgumentException otherwise. Keep public OrderPlaced record constructor and existing application result shape unchanged.
Test accepted/rejected decisions, fact creation, after-modification and after-payment snapshots, before-/after-publication failure windows, historical snapshot stability and local workflow occurrence semantics.
Keep OrderPlaced as returned in-process fact. Do not implement Kafka, event publisher, aggregate pending-event list, Outbox, CDC, durable event store or versioned integration schema without evidence.

## Alternatives
Retain arbitrary from(): simple but permits mislabeled historical placement total; rejected.
Move fact construction to aggregate.place(): could express candidate fact but risks confusion of candidate with successfully published state; not necessary currently.
Make returned fact an integration event immediately: would create unjustified semantics and transport guarantees.
Create event sourcing now: unnecessary storage and replay architecture before durable persistence.

## Consequences
Factory now refuses evolved snapshots. Normal PlaceOrderService works unchanged because it derives fact from initial Order after save.
This is a factory contract, not a proof that the record constructor is inaccessible or that any caller cannot fabricate facts; trust depends on controlled provenance.
Fact absence from a failed application call does not prove no in-memory publication happened; post-publication exception is a negative control.
Never infer persistent event history, financial charge, exactly-once delivery, or crash recovery from an OrderPlaced record.

## Revisit
When persisted authority, event identity, historical audit, independently consuming services, messaging, reliable publication or external payment effects create concrete requirements, update the semantics and apply the appropriate mechanism after proof.
