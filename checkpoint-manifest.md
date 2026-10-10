# Checkpoint Manifest — C1.1-P15 Candidate

## Identity

- Target checkpoint: C1.1-P15
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P14
- Inherited branch: checkpoints/C1.1-P14
- Inherited commit: 4b512248e93609bc7e1529223ebbbf4d5b11489b

## Engineering question

When the same logical business intent appears again, what should repeated execution mean, how can the system know it is a replay, and which business effects/results must remain stable?

## Part classification

Type C — Repeated-Command / Replay Failure Semantics.

## Production code/config

Unchanged.

## Required distinctions

duplicate request != same logical command.

same payload != same logical command.

effect convergence != duplicate detection.

duplicate suppression != result replay.

same final state != at-most-once execution.

correlation ID != idempotency key.

local state idempotence != external side-effect idempotence.

## Controlled experiments

1. lost cancellation success response followed by retry;
2. concurrent identical cancellations;
3. repeated refund request.

## Intentionally deferred

- duplicate payment execution: P16;
- general command/idempotency key model;
- durable dedup/result storage;
- key expiry;
- same-key different-payload detection;
- restart-safe replay;
- external exactly-once claims.

The complete idempotency mechanism belongs to later roadmap work.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P14 tests remain green;
3. sequential cancellation retry returns a different outcome while final state remains CANCELLED;
4. concurrent identical cancellation performs two saves and both calls can return Accepted;
5. final current Order still contains one cancellation occurrence;
6. repeated refund is suppressed without replaying original success;
7. no duplicate-payment experiment or idempotency mechanism is introduced prematurely.
