# ADR-0011 — Assemble complete immutable Order successor before local publication

**Status:** Accepted for independent V8 Cluster 1.1 Part 1.1.11.

## Context

P06–P10 already express guarded Order transitions, a P07 postcondition contract and a P10 full-history business-invariant checker. The V8 P11 requirement is to make **logical atomicity explicit** and test failure **between related state changes**, while deliberately avoiding PostgreSQL, transactions and premature distributed machinery.

An accepted cancellation must not expose `status=CANCELLED` without its `ORDER_CANCELLED` local fact, or vice versa. But local completeness of one new Order value cannot be confused with atomic shared-state read–guard–save, multi-object update or client acknowledgment.

## Decision

1. Keep the Order immutable. Calculate the target status, prepare a private new occurrence list, validate the new complete Order and P07 postconditions, and only then return it for application publication.
2. Add package-private `Order.assembleTransition` with `AssemblyProbe` and three `AssemblyStage` values for **deterministic tests only**. All existing public Order business methods use the production no-op probe.
3. Inject failures after target decision, after occurrence staging and after successor construction. Verify old Order remains unchanged in every case.
4. Compare with a deliberately unsafe **test-only mutable split fixture** showing `CANCELLED` without the fact after an injected interruption. Do not adopt this fixture in production code.
5. Add repository failure tests **before** and **after** publication, including the critical distinction that exception-after-write leaves an accepted local state despite caller-visible failure.
6. Add test-only Order+independent-audit failure and stale-successor overwrite to preserve unresolved multi-object/concurrent fragility.
7. No `@Transactional`, transaction manager, PostgreSQL, optimistic lock, distributed lock, message broker, outbox or Saga is introduced.

## Alternatives evaluated

- **Mutate status then append fact in the existing Order:** reject; failure between operations exposes a partial state, and observers could read it.
- **Append fact then mutate status:** reject; reverses the partial inconsistency but does not eliminate it.
- **Catch exception and try to undo individual mutable writes:** reject; rollback can itself fail, intermediate state may have been seen, and compensating external effects are not literal undo.
- **Build immutable successor first (selected):** simplest local model; validation before reference publication ensures the single returned Order is complete.
- **Wrap the whole method in `synchronized`:** not justified; would not deliver cross-process, database or external atomicity, and needs exact shared-lock scope.
- **Add `@Transactional` with no database:** reject as a misleading claim. Framework annotation is not a proof of resource enlistment or transaction boundary.
- **Introduce PostgreSQL and versioned compare-and-swap now:** defer; later atomicity/persistence/concurrency chapters will establish scope and evidence.
- **Saga or distributed transaction:** premature; no real independent external effects exist in this project yet.

## Guarantees, tradeoffs and reversal criteria

**Supported:** No normal production transition method mutates the source Order. Faults injected during assembly leave it unchanged; successful returned successor contains both target status and accepted occurrence, passing P07/P10 contracts. Controlled repository tests distinguish before-save and after-save failures.

**Not supported:** Local whole-operation atomicity under concurrent read/guard/save, multi-object atomicity, durable persistence, exactly-once acceptance or reliable acknowledgment, SQL transaction semantics, distributed agreement, provider capture/refund or real kitchen work. Public learning-action HTTP route remains unauthenticated.

**Costs:** Adds a package-private test seam and three stage labels; tests must prevent misinterpretation as production fault injection. Replaying P10 history at each construction remains potentially O(n) and has no load benchmark.

**Revisit when:** introduced persistence/rehydration, versioned conditional writes, new co-committed business facts, real service decomposition, external payment/restaurant authoritative effects, or evidence requiring durable/local transaction guarantees.
