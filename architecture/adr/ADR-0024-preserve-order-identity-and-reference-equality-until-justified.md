# ADR-0024 — Preserve Domain Identity and Current Java Reference Equality

Status: Accepted for C1.2-P05 scope, revisitable with evidence.
Inherited: checkpoints/C1.2-P04, 0c3efa497887930618e6ab9dc3b7503b09c5ceb8.

## Context
Order represents one continuing purchase through immutable successor states. Its stable OrderId is a UUID value-object record. Java Order.equals/hashCode remains inherited from Object. P02 proved normal identity continuity, P03 distinguished domain successor and authoritative publication. P17 demonstrated stale same-ID state; P18 demonstrated in-memory authority loss.

## Decision
Keep OrderId as the explicit business identity, keep normal immutable successor construction and leave Order.equals/hashCode reference-based for now. Verify the distinction in executable tests and explain its collection semantics, rather than changing equality blindly in a conceptual part.

## Alternatives and reasoning
- Identity-based Order.equals: attractive for comparing business identity, but does not identify authoritative version and could collapse distinct immutable snapshots in HashSet<Order>. Evaluate based on actual collection and persistence use.
- All-field Order.equals: conflates entity identity with mutable lifecycle state; structurally equal distinct purchases can be misinterpreted if identity omitted.
- Object-reference identity as business identity: fails at reconstitution and immutable successor replacement.
- ORM-managed entity identity/JPA @Id/@Version now: premature; no JPA persistence yet.
No proposed equals strategy can prevent a stale save without authoritative repository control.

## Non-guarantees
No guarantee of globally unique UUID, command deduplication, durable Order reconstruction, JPA proxy equality, entity conflict serialization, provider payment identity, or account/restaurant authority.
The conclusion addresses implemented Order semantics only: Customer, Restaurant, Payment full entities are not yet implemented.

## Revisitation triggers
New use of Order in sets/maps, ORM/persistence mapping choice, invariants involving entity equality, domain identity migration, required event-snapshot comparison or association rules.
