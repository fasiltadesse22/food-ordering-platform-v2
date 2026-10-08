# ADR-0007 — Add Domain Guards and Explicit Transition Contracts

Status: Accepted for C1.1-P07

## Context

C1.1-P06 proved source-state legality:

PLACED → ACCEPTED
PLACED → REJECTED
PLACED → CANCELLED
ACCEPTED → PREPARING
PREPARING → COMPLETED

However, source state alone is insufficient for business correctness.

At the P06 checkpoint:
- any application caller holding an OrderId could invoke restaurant acceptance/rejection/preparation/completion because no acting restaurant identity was supplied;
- any application caller holding an OrderId could invoke cancellation because no acting customer identity was supplied;
- REFUND_REQUESTED could be recorded without proving that payment had been recorded or that the Order was in a refund-relevant lifecycle branch.

The P06 checkpoint is preserved as the fragile pre-guard baseline.

## Decision

Add contextual domain guards while retaining the P06 state machine.

Restaurant-side lifecycle operations require a RestaurantId matching the Order's restaurant.

Customer cancellation requires a CustomerId matching the Order's customer.

Refund request requires:
1. Order lifecycle state is REJECTED or CANCELLED; and
2. PAYMENT_RECORDED exists in the current workflow trace.

Introduce OrderGuardViolationException with explicit codes.

Successful transition contracts are verified through tests:
- business identity remains unchanged;
- customer/restaurant ownership remains unchanged;
- stable order content remains unchanged;
- expected target state is reached;
- exactly one expected workflow occurrence is appended;
- original immutable representation remains unchanged.

## Guard evaluation order

For lifecycle transitions:
1. validate structural source-state legality;
2. evaluate contextual guard;
3. evolve state and append occurrence.

This intentionally separates:
- state-machine legality;
- contextual business eligibility.

## Validation is not guard logic

CustomerId and RestaurantId constructors reject blank identifiers.

That is structural/value validation.

A nonblank RestaurantId referring to another restaurant is syntactically valid but fails the ownership guard.

Therefore:

validation != guard.

## Authentication is not provided by this decision

The application use case accepts typed customer/restaurant identity as claimed business context.

P07 does not prove:
- who authenticated;
- that the caller cannot forge an ID;
- that Spring Security supplied the identity;
- that a token or session is trustworthy.

A later security mechanism must provide trustworthy actor identity. The domain guard answers what must match once such identity is available.

## Refund policy scope

P07 authorizes REFUND_REQUESTED only when:
- payment was recorded locally; and
- Order is REJECTED or CANCELLED.

This is a selected precondition for the current workflow, not a complete payment/refund model.

It does not mean a refund completed.

## Consequences

Positive:
- source-state legality is no longer mistaken for sufficient authorization;
- ownership context becomes explicit at the application/domain boundary;
- refund request cannot be recorded for an unpaid Order or unrelated lifecycle branch;
- failed guards leave authoritative repository state unchanged;
- postconditions become explicit and testable.

Negative:
- callers must supply additional business context;
- trusting caller-supplied identity without authentication would remain unsafe;
- guard checks still operate on potentially stale Order state;
- repeated-command semantics remain unresolved.

## Alternatives rejected

### Keep guards in the controller/application only

Rejected because another adapter could bypass lifecycle ownership rules.

### Introduce Spring Security now

Rejected because P07 is defining domain preconditions, not implementing the security architecture.

### Add payment service/provider verification

Rejected because real payment integration is not yet justified.

## Evolution criteria

P08 will deepen terminal/reversible/irreversible and compensation semantics.

P09 will deepen business failure versus technical failure.

Later concurrency parts will test whether guards evaluated against stale state remain sufficient.
