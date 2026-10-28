# ADR-0010 — Enforce Order semantic invariants independently of input validation and future SQL constraints

**Status:** Accepted for independently rebuilt V8 Part 1.1.10.

## Context

The V8 blueprint requires separating *meaning* (what business states and timelines may exist) from request validation and future database integrity mechanisms. P09 has typed business denial/HTTP translation, P07 pre/post guards, and P08 terminality. A status enum plus structurally nonnull occurrences could still tell an impossible lifecycle story if imported/corrupted or constructed through another future code path.

## Decision

- Add `OrderBusinessInvariants.requireValid(status, workflowOccurrences)`, a package-local, domain-owned historical consistency checker over immutable accepted local occurrences.
- Invoke it after snapshot fields are assigned in the Order constructor for placement and every accepted immutable successor.
- Report violations as `OrderInvariantViolationException`, an **internal technical integrity defect**, not a user business refusal or 400 invalid HTTP request.
- Keep existing P07 operation-local guards and postcondition checks. Do not replace them with a historic replay check alone: guard decides admission; invariant evaluates the whole accepted state.
- Keep HTTP-input validation in existing controller; add a **distinct** Spring validation suite alongside a domain invariant suite.
- Document `NOT NULL`, `CHECK`, FK and uniqueness only as **possible future mechanisms**. Do not introduce PostgreSQL, migrations, optimistic locking or any external storage.
- Explicitly preserve concurrency race and crash loss; the checker verifies a *representation*, not shared-state atomicity or persistence durability.

## Alternatives

1. **Controller-only** — rejected: future non-HTTP callers or domain reconstitution could bypass input checks.
2. **SQL CHECK defines the rule** — rejected: no database exists, and ordinary row-level constraints do not by themselves determine ordered workflow legality, preceding payment, or external cross-entity truth.
3. **Only P07 guards, no accepted-state assessment** — inadequate for hypothetical corrupt/imported snapshots whose final enum is individually valid but history is impossible.
4. **Duplicate mutable status flag** — rejected: separate writable projection risks drift; derive consistency by replaying the currently accepted local history.
5. **Event-sourcing framework now** — rejected as premature. Local occurrence list is not a durable append-only log.
6. **Cross-service verification** — premature: no authoritative remote payment/inventory effects exist yet.

## Cost and reversal triggers

The checker scans entire history for each newly constructed Order; a long trace could incur repeated O(n) work with many transitions. Tests prove functionality, not performance at scale. Revisit with durable reconstitution, partial histories, migration/backfill, large history size, concurrency transactions, external state authority, or new business rules. A future design may need incremental state proofs plus a durable history integrity mechanism instead of repeated full replay.

## Warning

These rules reflect the *selected provisional* P06/P07 workflow policy; the invariant's implementation must change along with formally approved business policy, not freeze an arbitrary assumption forever.
