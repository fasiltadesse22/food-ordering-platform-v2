# C1.1-P15 Experiment — Repeated Commands and Replay Semantics

## Question

How does the current application behave when the same cancellation/refund intent is submitted again, and does convergent final state imply duplicate detection or replay-safe semantics?

## Execution

GitHub Actions run:
38032248389

Command:

mvn -B -ntp verify

## Observed test result

OrderRepeatedCommandReplayTest:
- tests run: 3
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 78
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Lost cancellation success + sequential retry — observed

First CANCEL:
Accepted(CANCELLED)

The test deliberately treats the first success response as lost and submits the same logical cancellation intent again.

Retry:
Rejected(ILLEGAL_TRANSITION)

Final authority:
CANCELLED

Current cancellation occurrences:
1

Repository saves:
2 total:
- initial PLACED seed;
- first successful cancellation.

The retry was rejected before another save.

Interpretation:
the selected state/effect converges, but the original success result is not replayed and the server has no logical command identity proving the retry is the same command.

## Concurrent identical cancellation — observed

A coordinated repository forced both calls to capture:
PLACED

Both calls returned:
Accepted(CANCELLED)

Observed repository behavior:
- reads: 2
- saves: 2

Final current authority:
CANCELLED

Final current cancellation occurrences:
1

Interpretation:
both duplicate executions actually ran and saved.

The final Order looks like one cancellation because each immutable successor was independently derived from the same original PLACED snapshot and one complete successor replaced the other.

Therefore:
same final state != at-most-once execution.

## Repeated refund request — observed

Setup:
PAYMENT_RECORDED
→ CANCELLED

First REFUND_REQUESTED:
Accepted(CANCELLED)

Second REFUND_REQUESTED:
Rejected(REFUND_ALREADY_REQUESTED)

Final workflow:
PAYMENT_RECORDED
ORDER_CANCELLED
REFUND_REQUESTED

Refund occurrences:
1

Interpretation:
a domain prior-effect guard suppresses another selected refund milestone.

It does not replay the first Accepted result and does not provide a general logical-command deduplication mechanism.

## Evidence interpretation

Executed and verified:
- sequential cancellation retry changes result from Accepted to ILLEGAL_TRANSITION while final state remains CANCELLED;
- concurrent identical cancellations can both execute, save and return Accepted;
- current final Order can converge to one cancellation occurrence despite two saves;
- repeated refund request is explicitly suppressed without replaying original success.

Structurally demonstrated:
- OrderWorkflowUseCase has no command ID/idempotency-key parameter;
- current application cannot distinguish retry of one logical command from a new same-looking attempt using explicit command identity.

Evidence-backed inference:
- state-machine terminality may make selected local transitions effect-convergent without providing duplicate detection;
- final-state inspection is insufficient to prove at-most-once execution;
- external independently committed effects require separate idempotency reasoning.

## Limitations

Not established:
- durable deduplication;
- replay after restart;
- idempotency-key scope/expiry;
- same-key different-payload conflict handling;
- at-most-once execution;
- exactly-once business effect;
- external payment safety.

## Forward boundary

Duplicate PAYMENT execution remains intentionally untested here.

P16 owns duplicate payment and external-effect reasoning.
