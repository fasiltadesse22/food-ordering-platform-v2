# ADR-0006 — Derive an Explicit Order Lifecycle State Machine from P05 Workflow Evidence

Status: Accepted for C1.1-P06

## Context

C1.1-P05 made several business workflow paths executable but deliberately allowed contradictory sequences.

Observed P05 fragility included:

PREPARATION_STARTED
→ RESTAURANT_ACCEPTED
→ RESTAURANT_REJECTED
→ ORDER_COMPLETED

The application also kept Order.status at PLACED throughout all workflow activity.

This proves that workflow discovery alone is insufficient to protect lifecycle correctness.

## Decision

Introduce an explicit Order lifecycle model with these states:

- PLACED
- ACCEPTED
- REJECTED
- CANCELLED
- PREPARING
- COMPLETED

Introduce these P06 transitions:

- PLACED → ACCEPTED
- PLACED → REJECTED
- PLACED → CANCELLED
- ACCEPTED → PREPARING
- PREPARING → COMPLETED

Any attempt to invoke one of those transition operations from a different source state is rejected with IllegalOrderTransitionException.

Payment and refund remain workflow milestones and do not become OrderStatus values.

## Why payment/refund are not Order states

P05 showed that payment/refund represent a financial dimension that can coexist with different Order lifecycle positions.

For example:

PAYMENT_RECORDED
→ RESTAURANT_REJECTED
→ REFUND_REQUESTED

If PAYMENT_RECORDED became a single OrderStatus state, the model would start mixing payment lifecycle and fulfillment lifecycle into one enum.

P06 therefore keeps:

Order lifecycle state
separate from
financial workflow milestones.

## Why the transition set is intentionally small

P06 only authorizes transitions supported by the P05 scenarios and current scope.

It does not yet decide:
- whether cancellation after acceptance should be allowed;
- whether rejected/cancelled/completed states are terminal in the full business sense;
- whether refund request is required under every paid rejection/cancellation;
- actor-specific contextual guards beyond state source;
- repeated-command semantics.

Those are later parts.

## Mechanism

OrderLifecycleTransition explicitly declares:
- source OrderStatus;
- target OrderStatus;
- workflow action;
- participant label.

Order.transition verifies that the current status equals the transition source before constructing the next immutable Order representation.

## Consequences

Positive:
- illegal sequencing becomes rejectable;
- lifecycle state now reflects business progress;
- transition rules are centralized and executable;
- P05 workflow trace remains available for diagnosis.

Negative:
- the state model is still intentionally incomplete;
- state-source checks are not the same as full business guards;
- no concurrency control prevents stale-state decisions;
- no persistence durability exists.

## Alternatives rejected

### Keep permissive workflow only

Rejected because P05 reproduced contradictory lifecycle behavior.

### Encode every workflow milestone as OrderStatus

Rejected because payment/refund are orthogonal concerns and would produce a mixed state dimension.

### Introduce a state-machine framework

Rejected because the current state graph is small and requires domain understanding, not infrastructure.

## Evolution criteria

P07 will deepen guards, preconditions and postconditions around transitions.

P08 will interpret terminal/reversible/irreversible semantics.

Later concurrency parts will test whether valid transitions can still conflict when based on stale state.
