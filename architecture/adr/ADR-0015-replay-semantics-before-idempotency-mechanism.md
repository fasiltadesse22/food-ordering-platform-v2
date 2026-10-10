# ADR-0015 — Define Replay Semantics Before Introducing an Idempotency Mechanism

Status: Accepted for C1.1-P15 candidate

## Context

P14 studied different conflicting intents:
CUSTOMER CANCEL versus RESTAURANT ACCEPT.

P15 studies a different problem:
the same logical intent may be submitted again.

Reasons include:
- response lost after commit;
- client timeout;
- user double-click;
- proxy/client retry;
- duplicate delivery;
- caller uncertainty about whether the first attempt committed.

The current OrderWorkflowUseCase carries business target/actor identity but no logical command identity or idempotency key.

Therefore the application cannot distinguish:
- retry of the same logical command;
- a new attempt that happens to have the same payload;
- two concurrent duplicate deliveries.

## Current observed semantic families

### State-machine convergence

Sequential cancellation:
- first CANCEL succeeds;
- second identical-looking CANCEL observes CANCELLED and returns ILLEGAL_TRANSITION;
- no second cancellation occurrence is stored.

The desired current state converges, but the retry does not replay the original successful result.

### Concurrent duplicate execution

Two identical cancellation calls can:
- both capture PLACED;
- both execute the domain transition;
- both save a CANCELLED successor;
- both return Accepted.

Final current state contains one cancellation occurrence because last complete successor replaces the other.

This is NOT deduplication.
Both executions happened.

### Explicit duplicate suppression

REFUND_REQUESTED has a domain guard:
REFUND_ALREADY_REQUESTED.

A second refund request is prevented from adding another refund occurrence.

But it returns a rejection rather than replaying the first successful result.

## Decision

P15 will NOT add a general idempotency-key store.

P15 will:
- define repeated-command/replay vocabulary;
- execute sequential lost-response retry behavior;
- execute concurrent identical cancellation behavior;
- execute explicit repeated-refund suppression;
- preserve the pre-idempotent state.

The dedicated later idempotency work remains responsible for:
- logical request identity;
- key scope;
- payload fingerprint/conflict;
- atomic effect + key/result recording;
- concurrent duplicate handling;
- retention/expiry;
- restart behavior;
- replaying original result where required.

## Why not solve it now

The V8 roadmap deliberately has later idempotency work whose project pressure is to evolve payment/order paths to tolerate repeated and concurrent duplicates and explicitly test:
- lost response then retry;
- concurrent identical requests;
- restart;
- key expiry;
- conflicting payload under the same key.

Implementing that complete mechanism in P15 would collapse future learning and violate the technology/mechanism authorization sequence.

## Required distinctions

duplicate transport request != same logical command.

same payload != same logical command identity.

same logical command != necessarily same HTTP request.

effect idempotency != duplicate detection.

duplicate suppression != result replay.

same final state != proof that only one execution occurred.

idempotent current-state mutation != idempotent external side effect.

correlation ID != idempotency key.

request ID != business command ID.

## Forward pressure

P16 studies duplicate payment and external-effect thinking.

That part must not infer that current cancellation convergence proves payment safety.

Later dedicated idempotency work may introduce a real idempotency mechanism after business-effect identity and atomicity boundaries are explicit.
