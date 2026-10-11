# ADR-0021 — Preserve Order Identity Continuity Without Premature Persistence or Versioning

Status: Accepted for C1.2-P02 scope (identity-contract clarification, not durable-authority mechanism)
Base: `checkpoints/C1.2-P01` at `2a61909a2cf3470b3917caab2abecdafe9334e81`.

## Context
P01 derived an Order-centered immediate consistency candidate from established invariants.
C1.1-P04 already proved typed ID value equality, multiple same-ID Order objects and detached stale snapshots.
P17 proved that same-ID stale representations can replace newer authority.
P18 proved that the same syntactically valid ID is absent after JVM replacement.
Identity continuity must be distinguished from immutable object identity, repository currentness, transactional validity and durability.

## Decision
Retain typed `OrderId(UUID)`, `CustomerId(String)` and `RestaurantId(String)`.
Retain immutable successor construction preserving the OrderId, and reference-based Java `Order.equals` semantics.
Retain the existing in-memory repository and deliberately unsafe unconditional save as before-state evidence.
Add deterministic executable identity/lifecycle/independent-authority tests plus a source-grounded contract/matrix.
Do not add a database, JPA annotations, version, transactional constraint, distributed service, or imagined reconstitution factory yet.

## Why
- One business identity can span multiple immutable Java representations.
- Identifier equality is a *lookup and continuity contract*, not state freshness or authorization.
- Each repository instance controls only its own process-local state.
- Identity stability will be necessary for durable reconstruction later, but the actual recovery mechanism is not yet justified as a P02 implementation.

## Alternatives
A. Use object reference as identity: cannot survive immutable evolution, serialization or reconstruction.
B. Use all-field value equality for Order identity: business state changes would make same continuing Order appear different.
C. Treat UUID as proof of global existence/uniqueness: unsupported; current put overwrites a colliding same-ID entry.
D. Introduce JPA @Id or @Version now: premature; P11–P17 own persistence/concurrency mechanics.
E. Separate repositories/remote services by ID type: topology follows no P02 evidence.

## Trade-offs / non-guarantees
Typed IDs have small conversion overhead and are not sufficient authorization proof. 
UUID random generation yields probabilistic uniqueness but no collision check or exact-once command semantics.
No cross-process shared authority, crash recovery, stale-write safety, relational uniqueness, entity equals contract, financial truth or complete lifecycle history guarantee has been added.

## Revisit triggers
Actual persistence/reconstitution requirements, collision handling, identity migration, interop with external identifiers, mapped ORM object equality trade-offs, security/identity-boundary introduction or changed Order domain identity semantics.

## Handoff
P03 derives atomic consistency requirements, P04 aggregate candidates, P05 formal entity equality.
P11–P12 will make durable identity continuity executable; P16–P17 will address authoritative version freshness.
