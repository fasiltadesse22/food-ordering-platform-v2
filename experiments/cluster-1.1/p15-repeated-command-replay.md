# C1.1-P15 Experiment — Repeated Commands and Replay Semantics

## Question

How does the current application behave when the same cancellation/refund intent is submitted again, and does convergent final state imply duplicate detection or replay-safe semantics?

## Hypothesis

The current application has no general logical command identity.

Therefore:
- sequential cancellation retry will be interpreted as a new CANCEL against CANCELLED and be rejected;
- concurrent identical cancellations can both execute and return Accepted;
- final cancellation state can converge even without deduplication;
- refund replay is explicitly suppressed by domain history but will return rejection rather than the original result.

## Predictions

### Lost cancellation response + sequential retry

First CANCEL:
Accepted(CANCELLED)

Retry:
ILLEGAL_TRANSITION

Final authority:
CANCELLED

Cancellation occurrences:
1

### Concurrent identical cancellation

Both capture:
PLACED

Both:
return Accepted(CANCELLED)

Repository saves:
2

Final authority:
CANCELLED

Current cancellation occurrences:
1

Interpretation:
the final representation converges, but duplicate execution was not prevented.

### Repeated refund request

Precondition:
PAYMENT_RECORDED
then CANCELLED

First refund request:
Accepted

Second:
REFUND_ALREADY_REQUESTED

Final refund occurrences:
1

## Controlled variables

Use the current real OrderWorkflowService.

Use:
- a counting repository for sequential replay;
- a shared-read barrier repository for concurrent identical cancellation;
- the real in-memory repository for refund replay.

No production idempotency mechanism is added.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P15 CI.

## Evidence interpretation

A successful P15 experiment would establish selected replay behavior of the current checkpoint.

It would not establish:
- durable deduplication;
- logical request identity;
- at-most-once execution;
- exactly-once external effects;
- replay after restart;
- idempotency-key expiry behavior.

## Forward boundary

Duplicate PAYMENT execution is intentionally not exercised here.

P16 owns duplicate payment and external-effect reasoning.
