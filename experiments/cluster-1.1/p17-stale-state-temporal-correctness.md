# C1.1-P17 Experiment — Stale-State Decisions and Temporal Correctness

## Question

Can a decision derived from a previously authoritative Order become unsafe after authority changes, even when there are no simultaneously executing threads at the moment the stale decision is finally applied?

## Execution

GitHub Actions run:
38034398881

Command:

mvn -B -ntp verify

## Observed test result

OrderStaleStateTemporalCorrectnessTest:
- tests run: 4
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 86
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## A — modification after payment — observed

Initial captured snapshot:
- status = PLACED;
- no PAYMENT_RECORDED.

Intervening authoritative change:
- current service recorded payment;
- current authority contained PAYMENT_RECORDED.

Delayed decision:
- modifyLines executed against the earlier pre-payment PLACED snapshot;
- stale modified successor was saved unconditionally.

Final authority:
- status = PLACED;
- replacement lines present;
- ORDER_MODIFIED present;
- PAYMENT_RECORDED absent.

Interpretation:
the stale successor erased a newer authoritative payment fact.

Important qualification:
the current domain does not encode "payment forbids modification" as a business guard.
P17 therefore does not claim modification-after-payment is universally illegal.
The verified stale-state defect is the erasure of the newer authoritative fact.

## B — payment after cancellation from stale snapshot — observed

Initial captured snapshot:
PLACED.

Intervening authoritative change:
- cancellation returned Accepted;
- current authority became CANCELLED + ORDER_CANCELLED.

Delayed decision:
- recordPayment executed against the earlier PLACED copy;
- stale paid successor was saved.

Final authority:
- status = PLACED;
- PAYMENT_RECORDED present;
- ORDER_CANCELLED absent.

Interpretation:
the stale successor resurrected an obsolete lifecycle state and erased the newer cancellation history.

## Fresh payment-after-cancellation control — observed

No stale snapshot was used.

Sequence:
- current cancellation succeeded;
- current recordPayment then succeeded against fresh CANCELLED authority.

Final authority:
- status = CANCELLED;
- workflow = ORDER_CANCELLED, PAYMENT_RECORDED.

Interpretation:
payment-after-cancellation is also currently allowed by business policy because recordPayment has no contextual guard.

This is distinct from stale-write corruption.

## C — fresh guard versus stale guard — observed

Two references captured the same PLACED Order.

Intervening authoritative change:
- restaurant acceptance succeeded;
- current authority became ACCEPTED.

Fresh authoritative Order:
- modifyLines rejected with MODIFICATION_REQUIRES_PLACED_ORDER.

Stale PLACED copy:
- the same modifyLines method passed its guard;
- stale modified successor was saved.

Final authority:
- status = PLACED;
- ORDER_MODIFIED present;
- RESTAURANT_ACCEPTED absent.

Interpretation:
the guard itself is correct for the object it evaluates.
The failure is that the object was no longer current.

## Core evidence

Executed and verified:
- stale modification can erase later payment history;
- stale payment can erase cancellation and resurrect PLACED;
- fresh payment-after-cancellation is separately allowed by current policy;
- fresh ACCEPTED state rejects modification while stale PLACED permits it;
- no simultaneous threads are required for any of these stale-state failures.

Evidence-backed inference:
a precondition check is insufficient when correctness depends on the state remaining current until authoritative commit/effect.

## Limitations

Not established:
- database MVCC/isolation behavior;
- optimistic-locking/version semantics;
- multi-JVM behavior;
- acceptable snapshot lifetime;
- final business policy for modification-after-payment;
- final business policy for payment-after-cancellation.

## Forward boundary

P17 preserves the unversioned failure state.

P18 next studies process crash, transient state and durability non-guarantees.

Cluster 1.2 later introduces real persistence where version-aware write protection can be selected and validated.
