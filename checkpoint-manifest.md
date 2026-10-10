# Checkpoint Manifest — C1.1-P15

## Identity

- Checkpoint: C1.1-P15
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P14
- Inherited branch: checkpoints/C1.1-P14
- Inherited commit: 4b512248e93609bc7e1529223ebbbf4d5b11489b
- P15 experiment commit: 12b96146a399f7d61d9639b7c153d4072f0a2b73
- P15 verification run: 38032248389

## Engineering question

When the same logical business intent appears again, what should repeated execution mean, how can the system know it is a replay, and which business effects/results must remain stable?

## Part classification

Type C — Repeated-Command / Replay Failure Semantics.

## Production code/config

Unchanged.

## Verified experiments

### Sequential cancellation replay

First:
Accepted(CANCELLED)

Retry:
Rejected(ILLEGAL_TRANSITION)

Final:
CANCELLED with one ORDER_CANCELLED occurrence.

### Concurrent identical cancellations

Both capture:
PLACED

Both return:
Accepted(CANCELLED)

Repository:
2 reads
2 saves

Final:
CANCELLED with one current ORDER_CANCELLED occurrence.

### Repeated refund request

First:
Accepted

Second:
REFUND_ALREADY_REQUESTED

Final:
one REFUND_REQUESTED occurrence.

## Verification

GitHub Actions:
38032248389

Observed:
Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Verified distinctions

duplicate request != identified replay.

effect convergence != duplicate detection.

duplicate suppression != result replay.

same final state != at-most-once execution.

same payload != proof of same logical command.

local state convergence != external-effect idempotence.

## Mechanisms deliberately absent

- command ID/idempotency key;
- deduplication store;
- stored original result;
- atomic effect + dedup record;
- retention/expiry;
- same-key payload fingerprinting;
- restart-safe replay;
- distributed duplicate suppression.

## Forward boundary

Duplicate payment is deliberately reserved for Part 1.1.16.

The later dedicated idempotency cluster owns the full mechanism.

## Next pressure

Part 1.1.16:
Duplicate Payment and External-Effect Thinking.
