# ADR-0004 — Treat the Order Repository as Current In-Process Order-State Authority

Status: Accepted for C1.1-P04

## Context

Part 1.1.4 asks two related questions:

1. What identifies the same business Order across time and across different Java objects?
2. When multiple representations of that Order exist, which one should application logic treat as current?

The project already has OrderId, OrderSnapshot, HTTP responses and OrderPlaced. Without an explicit authority rule, any one of these could be mistaken for the current source of truth.

## Decision

Within the current one-process architecture:

- OrderId is the stable business identity used to address an Order.
- OrderRepository is the application boundary for current Order state.
- InMemoryOrderRepository is the current runtime implementation of that authority.
- OrderSnapshot is a detached point-in-time observation.
- OrderResponse is a transport representation of an observation.
- OrderPlaced is a fact about an accepted occurrence, not the current-state store.
- Java object reference identity is not business identity.

The repository methods are named saveCurrent and findCurrentById to make this model explicit.

## Authority scope

This is deliberately narrow.

The repository is authoritative only for the application's current in-process Order representation.

It is not automatically authoritative for:
- customer-account truth;
- restaurant/menu truth;
- payment truth;
- durable historical truth after a process crash;
- future distributed participants.

## Why not introduce a database now?

The P04 question can be answered with the current in-memory adapter.

Introducing PostgreSQL now would mix identity/authority semantics with:
- database primary keys;
- transaction boundaries;
- locking;
- constraints;
- persistence durability.

Those belong to later parts and Cluster 1.2.

## Consequences

Positive:
- business identity is separated from Java object identity and HTTP/string representation;
- detached snapshots are explicitly non-authoritative;
- stale-copy reasoning becomes executable;
- later persistence can replace the adapter without changing the identity model.

Negative:
- current authority disappears on restart;
- saveCurrent is concurrency-naive;
- no version/optimistic-lock check exists;
- a same-ID replacement becomes current even if it was derived from stale data.

These weaknesses are intentionally preserved for later experiments.

## Reversal / evolution criteria

Cluster 1.2 may replace the in-memory authority with PostgreSQL while preserving the same business identity semantics.

Later concurrency work may add version-aware replacement rules, but P04 does not pre-authorize them.
