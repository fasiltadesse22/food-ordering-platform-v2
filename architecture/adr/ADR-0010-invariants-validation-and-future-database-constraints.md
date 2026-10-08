# ADR-0010 — Separate Business Invariants, Boundary Validation and Future Database Constraints

Status: Accepted for C1.1-P10 candidate

## Context

By C1.1-P09 the application had executable lifecycle rules, guards, failure semantics and several constructor checks, but those checks were not yet classified clearly.

Examples already present:
- CustomerId rejects blank values;
- OrderLine rejects non-positive quantity and negative price;
- Order state transitions prevent CANCELLED -> PREPARING;
- no database exists, so no persistence constraint is currently enforcing anything.

Without an explicit distinction it is easy to say "validation", "invariant" and "constraint" as if they were interchangeable.

They are not.

## Decision

### Boundary validation

Use Jakarta Bean Validation at the HTTP adapter to reject malformed request shape before constructing application/domain objects.

Examples:
- customerId must be nonblank;
- restaurantId must be nonblank;
- order line list must be present and nonempty;
- menu item id/name must be nonblank;
- quantity must be positive;
- unit price must be nonnegative.

These are adapter/input-shape checks. They do not define lifecycle truth.

### Domain validation

Value objects continue to reject structurally meaningless values even when created outside HTTP.

Examples:
- CustomerId cannot be blank;
- RestaurantId cannot be blank;
- OrderLine quantity must be positive;
- OrderLine price must not be negative.

This prevents non-HTTP callers from bypassing basic structural validity.

### Business invariants

Introduce OrderInvariants as a domain defense-in-depth verifier.

The verifier checks consistency between current OrderStatus and the accumulated workflow facts.

Examples:
- ORDER_CANCELLED history requires current state CANCELLED;
- RESTAURANT_REJECTED history requires current state REJECTED;
- ORDER_COMPLETED history requires current state COMPLETED;
- PREPARATION_STARTED history requires PREPARING or COMPLETED state;
- PREPARING requires acceptance and preparation facts;
- COMPLETED requires acceptance, preparation and completion facts.

Public commands should normally be rejected by lifecycle/guard rules before an invariant-invalid Order can be created.

If the invariant verifier ever detects contradictory internal state/history, it is treated as a programming/data-integrity defect, not a normal business rejection.

### Future database constraints

Do not introduce PostgreSQL in P10.

Document candidate persistence constraints for Cluster 1.2 only.

Likely row-level candidates:
- orders.id PRIMARY KEY / NOT NULL;
- orders.customer_id NOT NULL;
- orders.restaurant_id NOT NULL;
- orders.status NOT NULL;
- order_lines.order_id NOT NULL;
- order_lines.quantity CHECK quantity > 0;
- order_lines.unit_price CHECK unit_price >= 0.

These constraints may reinforce structural persistence integrity.

They do not define the full business invariants.

## Why database constraints are not the invariant

A database CHECK can ensure:

quantity > 0

but it cannot by itself explain:
- why quantity must be positive;
- who may modify an Order;
- whether CANCELLED may enter PREPARING;
- whether refund requires prior payment;
- whether the caller is the owning restaurant.

A persistence mechanism can enforce part of a rule without being the semantic rule.

## Why duplicate enforcement can be useful

The same structural property may be protected at multiple layers:

HTTP validation
→ domain value object
→ future database constraint

This is defense in depth, not semantic duplication, as long as each layer's responsibility is understood.

## Why no database now

Cluster 1.2 explicitly introduces PostgreSQL, migrations and transaction/consistency-boundary experiments.

Adding a database in P10 would collapse:
- semantic invariant reasoning;
- persistence constraint reasoning;
- transaction-boundary reasoning

into one step and destroy the current in-memory comparison point.

## Consequences

Positive:
- rule categories are explicit;
- malformed requests fail early at the adapter;
- non-HTTP callers still receive domain structural validation;
- internal state/history coherence is defended independently of transport;
- future database constraints have an explicit authorization map.

Negative:
- some structural checks exist at more than one layer;
- invariant verification adds runtime checks;
- future schema work must keep persistence constraints aligned with domain semantics.

## Preserved fragilities

- no durable persistence;
- no database constraint is currently executed;
- paid-but-PLACED modification remains possible;
- payment-after-cancellation remains possible;
- concurrency/staleness remain unsolved.

## Evolution

P11 will study logical atomicity and local consistency requirements.

Cluster 1.2 will later compare application/domain enforcement with real PostgreSQL constraints, transactions and optimistic concurrency.
