# ADR-0017 — Preserve Stale-Decision Failure Before Introducing Version Control

Status: Accepted for C1.1-P17 candidate

## Context

P13 and P14 proved overlapping operations can conflict when they capture the same Order state.

P17 studies a broader temporal problem:

A state can be authoritative when observed,
then authority can change,
then a previously valid decision can be applied later without concurrency at the moment of application.

The current model:
- returns immutable Order references/snapshots;
- has no version number;
- has no expected-version save;
- allows saveCurrent to replace current authority unconditionally.

Therefore a previously authoritative Order can remain usable after it becomes stale.

## Decision

Preserve production behavior unchanged.

Add deterministic sequential experiments that explicitly capture:
observation
→ decision assumption
→ intervening authoritative change
→ delayed application
→ resulting authoritative state.

Do not add:
- version fields;
- optimistic locking;
- compare-and-set;
- pessimistic locks;
- transactions;
- distributed coordination.

The stale before-state must remain visible for later persistence/concurrency evolution.

## Required distinction — stale data versus stale decision

Stale data:
a representation no longer reflects current authority.

Stale decision:
an action is executed using assumptions derived from an older representation after those assumptions may have been invalidated.

Holding stale data is not automatically a correctness failure.

Acting on it without revalidation/conflict detection can be.

## Experiment A — modification after payment

Capture a PLACED Order with no PAYMENT_RECORDED.

Intervening change:
record payment against current authority.

Delayed action:
modify lines using the earlier snapshot and save it.

Observed risk under current last-write-wins semantics:
the stale successor can erase the later PAYMENT_RECORDED fact.

Important qualification:
current production domain does not yet encode "payment makes modification illegal" as a business guard.

Therefore P17 does NOT claim that modification-after-payment is universally forbidden.

The stale-state failure is stronger and independent:
saving the pre-payment successor can erase a newer authoritative fact.

## Experiment B — payment after cancellation

Capture PLACED.

Intervening change:
current authority becomes CANCELLED.

Delayed action:
record payment on the earlier PLACED snapshot and save it.

Risk:
the stale successor can resurrect current status to PLACED and erase ORDER_CANCELLED.

Separately, a fresh recordPayment against CANCELLED is currently allowed because payment is an unrestricted milestone.

That is a missing/undecided business policy, not itself a stale-state defect.

## Experiment C — guard correctness on fresh versus stale copy

Capture two references to the current PLACED Order.

Intervening change:
restaurant acceptance makes current authority ACCEPTED.

Fresh authority:
modifyLines correctly rejects because modification requires PLACED.

Stale copy:
the same modifyLines guard sees PLACED, succeeds, and can be saved over ACCEPTED.

This demonstrates:
a correct guard evaluated against stale state is not sufficient for global temporal correctness.

## Temporal correctness rule

A precondition checked at observation/decision time is only useful while the facts on which it depends remain valid.

If correctness requires the condition to hold at authoritative commit/effect time, the mechanism must revalidate or detect that authority changed.

## Why no fix in P17

P17 establishes the failure and required property.

The eventual mechanism may involve:
- version comparison;
- conditional update;
- optimistic locking;
- serialization;
- another authority-specific tactic.

The correct choice should be made when Cluster 1.2 introduces real persistence and consistency boundaries.

## Forward consequences

P18 studies process crash, transient state and durability non-guarantees.

Later persistence work can use P17's deterministic tests as before-state evidence for version-aware writes.
