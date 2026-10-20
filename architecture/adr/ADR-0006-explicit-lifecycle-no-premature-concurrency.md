# ADR-0006 — Explicit fulfillment transitions without premature distributed coordination

Status: **Accepted provisionally for new V8.0 P06 lineage**.

## Context

P05 records a rich workflow but permits preparation before restaurant acceptance, acceptance followed by rejection, and completion after rejection while Order.status stays PLACED. It was a deliberate negative baseline. The Part 1.1.6 roadmap requires explicit lifecycle legality and tests for forbidden edges.

## Decision

Extend `OrderStatus` with PLACED, ACCEPTED, PREPARING, COMPLETED, REJECTED and CANCELLED. Map each existing workflow action into a guarded Order domain operation returning a new immutable Order on acceptance, and `InvalidOrderTransitionException` on denial. Keep payment recording and refund request as orthogonal once-only milestones. Commit Order successor through existing application repository port; no new services.

## Alternatives rejected

1. **Only an enum, no guards:** labels possible states but allows every transition and does not close P05's known bug.
2. **Guard only in a controller/service:** other adapters may bypass policy. Domain boundary must own lifecycle legality.
3. **One giant cross-product enum including payment/refund:** conflates independent authority and causes combinatorial explosion.
4. **State machine framework now:** not necessary for six states and seven triggers; obscures basic mechanism and adds dependencies without measurable force.
5. **Database transaction/locking now:** might solve later concurrent stale decisions but P06 is deliberately a sequential-state legality experiment.
6. **Kafka/Saga orchestration:** changes communication topology without deciding business legality.

## Consequences

- Previously recordable P05 contradiction is now denied by the domain.
- Legal happy/rejection/cancellation paths produce explicit fulfillment states.
- A refused transition appends no workflow occurrence and triggers no repository save on normal application path.
- Refund requests do not make a terminal fulfillment lifecycle active again.
- Payment history is local; no provider truth or idempotent external financial effect.
- P05 historical tests must be **evolved** on the new branch; old checkpoint never rewritten.
- Multiple concurrent legal commands can still overwrite each other via stale read–decide–save. No durable state or multi-process control.

## Reversal criteria

Revisit these guards when stakeholders clarify actual payment-before-preparation policy, acceptance reversals, special cancellation rights, post-completion disputes, refund exceptions, or expanded fulfillment/ delivery semantics. Revisit concurrency only when later V8 parts define reproducible races and invariants, not merely because a synchronized collection is available.

## Evidence

New P06 Java unit/application tests and GitHub Actions verification will be recorded in the checkpoint evidence file, with exact run identifiers and counts. This ADR is a design choice, not itself runtime proof.
