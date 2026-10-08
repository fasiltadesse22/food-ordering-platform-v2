# ADR-0008 — Make Terminality and Compensation Semantics Explicit

Status: Accepted for C1.1-P08

## Context

C1.1-P07 enforced guards and pre/postconditions but still left several semantic questions implicit:

- Which Order states are terminal for the lifecycle?
- Does terminal mean absolutely no later business action can happen?
- Does refund undo payment?
- Can a cancelled/rejected/completed Order be modified?
- What prevents a second refund request?
- Is "reverse" the same as "compensate"?

## Decision

Classify these Order lifecycle states as terminal in the current model:

- REJECTED
- CANCELLED
- COMPLETED

Terminal means: no further OrderStatus transition is currently modeled from that state.

Terminal does not mean no later workflow action can occur.

A paid REJECTED or CANCELLED Order may still append REFUND_REQUESTED. The lifecycle remains terminal.

PAYMENT_RECORDED is never erased by refund-related behavior. REFUND_REQUESTED is a new compensating occurrence.

Therefore:

reversal != compensation

and

refund != erasing payment history.

A second REFUND_REQUESTED is rejected by the current P08 semantic guard. This is not yet idempotent retry handling; P15 will address repeated-command semantics.

Introduce Order modification while PLACED so terminal-state modification can be tested explicitly.

Modification:
- requires owning customer;
- requires PLACED lifecycle state;
- keeps lifecycle state PLACED;
- replaces order lines;
- appends ORDER_MODIFIED.

## Important preserved fragility

PAYMENT_RECORDED does not move the Order out of PLACED.

Therefore a paid but still PLACED Order can currently be modified.

P08 does not fix this.

That behavior is intentionally preserved for the later modification-after-payment/stale-decision work.

## Irreversible effects

Within the current learning model:

- PAYMENT_RECORDED is treated as historical financial effect evidence that cannot be erased by a refund request.
- PREPARATION_STARTED represents a physical/business effect that cannot be literally undone by changing state backward.
- ORDER_COMPLETED is a terminal lifecycle outcome.

P08 does not claim a real external charge, real kitchen action or completed refund occurred. These are local model semantics.

## Alternatives rejected

### Reopen terminal Orders

Rejected because no current business requirement justifies it.

### Delete PAYMENT_RECORDED when refund is requested

Rejected because that destroys historical truth and incorrectly models compensation as rollback.

### Introduce Saga compensation

Rejected because there are no distributed transaction participants yet.

## Evolution criteria

P09 will distinguish business rejection from technical failure.

P15 will revisit repeated refund requests under command identity/idempotency semantics.

P17 will use the preserved paid-but-PLACED modifiability to study modification after payment and stale decisions.
