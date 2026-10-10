# ADR-0013 — Preserve the Concurrent Conflict Before Adding Coordination

Status: Accepted for C1.1-P13 candidate

## Context

The current application workflow is:

findCurrentById
→ evaluate transition/guard against the returned Order
→ create a new immutable successor
→ saveCurrent(successor)

The repository uses ConcurrentHashMap, but the whole read-decision-write sequence is not one atomic operation.

P12 explicitly classified concurrent safety as a non-guarantee.

P13 must now test that fragile state rather than immediately adding:
- synchronized;
- locks;
- versions;
- compare-and-set;
- PostgreSQL transactions;
- optimistic locking;
- distributed locks.

## Decision

Preserve production behavior unchanged.

Add a deterministic test-only repository that can:
1. force two operations to read the same PLACED Order;
2. hold both reads until both have captured the same authoritative snapshot;
3. force either successor to save first;
4. observe both application results and final authority.

Use restaurant ACCEPT and restaurant REJECT as the generic P13 conflicting pair.

Reserve the cross-actor CUSTOMER CANCEL versus RESTAURANT ACCEPT race for P14.

## Control

Sequential:
PLACED
→ ACCEPT
→ fresh read sees ACCEPTED
→ REJECT is ILLEGAL_TRANSITION

This verifies that the lifecycle rules work when the second operation observes current authority.

## Conflict experiment

Concurrent:
A reads PLACED
B reads PLACED

A derives ACCEPTED from PLACED
B derives REJECTED from PLACED

Both local decisions are valid relative to the state they observed.

If A saves then B saves:
- A returns Accepted(ACCEPTED);
- B returns Accepted(REJECTED);
- final authority is REJECTED;
- A's accepted workflow occurrence is absent from final authority.

If B saves then A saves:
- both callers still receive Accepted;
- final authority is ACCEPTED;
- B's rejected workflow occurrence is absent from final authority.

## Interpretation

The state machine is not broken.

The concurrency window is between:
- authoritative read; and
- authoritative replacement.

Each operation validates against a stale-but-once-authoritative PLACED representation.

The current repository applies last-write-wins replacement without checking whether the authority changed after the read.

## What this demonstrates

- local validity does not imply globally serializable history;
- immutable Orders do not prevent stale replacement;
- ConcurrentHashMap protects map operations, not the business transaction;
- last-write-wins can erase an accepted decision;
- final state depends on interleaving.

## What it does not yet justify implementing

P13 does not choose the correction mechanism.

Possible later mechanisms include:
- serialization in one process;
- version/compare-and-set;
- optimistic concurrency;
- pessimistic locking;
- database isolation/locking.

Mechanism choice depends on future persistence, contention and consistency-boundary evidence.

## Preserved fragility

The real repository remains vulnerable to this conflict.

The deterministic test is the preserved before-state.

P14 will apply the same mechanism to the required cancellation/acceptance race with different actors and business consequences.
