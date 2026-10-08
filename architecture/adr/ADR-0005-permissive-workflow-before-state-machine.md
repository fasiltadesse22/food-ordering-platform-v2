# ADR-0005 — Record Workflow Milestones Before Formalizing a State Machine

Status: Accepted for C1.1-P05

## Context

Part 1.1.5 must discover the end-to-end ordering workflow across customer, payment, restaurant and platform responsibilities before Part 1.1.6 formalizes legal/illegal lifecycle transitions.

Jumping directly to a complete status enum and transition table would turn assumptions into implementation before alternative paths, handoffs and policy questions are visible.

## Decision

Introduce a deliberately permissive in-process workflow trace.

The current Order may record these post-placement milestones:

- PAYMENT_RECORDED
- RESTAURANT_ACCEPTED
- RESTAURANT_REJECTED
- ORDER_CANCELLED
- REFUND_REQUESTED
- PREPARATION_STARTED
- ORDER_COMPLETED

Each occurrence also records the business participant label involved in the discovered handoff.

The trace does not yet reject contradictory or out-of-order milestones.

Order.status deliberately remains PLACED throughout P05.

## Why

This preserves the distinction:

workflow != state machine

P05 answers:
- what activities occur;
- which participant is involved;
- what alternative branches exist;
- where handoffs and financial consequences appear;
- which policy questions remain unresolved.

P06 will answer:
- which lifecycle states exist;
- which transitions are legal;
- which are illegal;
- what guards are required.

## Important non-claims

The P05 workflow recorder is not:
- an event store;
- Event Sourcing;
- a Kafka/event-bus implementation;
- a durable audit log;
- proof that every recorded sequence is business-valid;
- an external payment integration.

PAYMENT_RECORDED is a local learning-stage workflow milestone. It does not perform a real charge.

## Preserved weakness

Because no state machine exists yet, the current implementation can record suspicious sequences such as:
- preparation before restaurant acceptance;
- acceptance followed by rejection;
- completion without validated prerequisites.

This weakness is intentional evidence for P06.

## Reversal / evolution criteria

Part 1.1.6 must replace permissive lifecycle interpretation with explicit state-machine legality while preserving the discovered workflow branches and their evidence.
