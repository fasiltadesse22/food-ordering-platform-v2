# ADR-0002 — Use In-Memory Persistence in Cluster 1.1

- Status: Accepted for C1.1
- Scope: initial correctness discovery
- Nature: intentionally temporary

## Context

Cluster 1.1 must expose workflow, state and invariant reasoning without letting database mechanisms define the model prematurely.

## Alternatives

1. In-memory repository behind an output port.
2. PostgreSQL immediately.
3. Mock repository only in tests.

## Decision

Use a real in-memory adapter behind an application output port.

## Why

- The application executes a real save/load path.
- Domain/application code does not depend on Spring Data or a schema.
- Durability and transaction limitations stay visible.
- Cluster 1.2 can introduce PostgreSQL specifically when consistency-boundary and transaction behavior becomes the learning target.

## Consequences

- Restart loses state.
- No database transaction/constraint/locking behavior exists.
- No persistence durability is claimed.
- The adapter must later be replaced/evolved without rewriting the domain.

## Reversal trigger

Cluster 1.2.
