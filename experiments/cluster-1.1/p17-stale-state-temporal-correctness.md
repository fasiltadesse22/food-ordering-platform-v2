# C1.1-P17 Experiment — Stale-State Decisions and Temporal Correctness

## Question

Can a decision derived from a previously authoritative Order become unsafe after authority changes, even when there are no simultaneously executing threads at the moment the stale decision is finally applied?

## Hypothesis

The current repository has no version or expected-state comparison.

If a caller retains an old immutable Order, another operation updates current authority, and the old copy is later evolved and saved:
- its local guards evaluate against old state;
- its successor can overwrite newer authoritative state/history.

## Predictions

### A — modification after payment

1. capture PLACED with no payment fact;
2. current service records payment;
3. evolve old copy with modified lines;
4. save old-copy successor.

Expected final current Order:
- modified lines;
- ORDER_MODIFIED present;
- PAYMENT_RECORDED erased.

### B — payment after cancellation using stale copy

1. capture PLACED;
2. current service cancels Order;
3. record payment against old PLACED copy;
4. save old-copy successor.

Expected final current Order:
- status PLACED;
- PAYMENT_RECORDED present;
- ORDER_CANCELLED erased.

### Fresh-payment control

1. cancel current Order;
2. invoke recordPayment through current service.

Expected:
- status remains CANCELLED;
- ORDER_CANCELLED + PAYMENT_RECORDED both present.

This proves payment-after-cancellation is also a current policy gap independent of staleness.

### C — two copies / stale guard

1. capture two PLACED references;
2. current service accepts Order;
3. fresh ACCEPTED Order rejects modifyLines;
4. stale PLACED copy accepts modifyLines;
5. save stale modified copy.

Expected final current Order:
- status PLACED;
- ORDER_MODIFIED present;
- RESTAURANT_ACCEPTED erased.

## Setup

Use:
- real InMemoryOrderRepository;
- real OrderWorkflowService for intervening authoritative changes;
- real Order domain methods on deliberately retained old immutable copies;
- direct real OrderRepository.saveCurrent for the delayed stale successor.

No concurrent threads are required.

This isolates time/staleness from scheduler interleaving.

## Controlled variable

The relevant difference is whether a decision/evolution is based on:
- fresh current authority; or
- an earlier once-authoritative snapshot.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P17 CI.

## Evidence interpretation

If predictions hold:
- a state can be valid when observed yet unsafe to use later;
- correct domain guards cannot protect against stale input on their own;
- unconditional save allows stale successors to resurrect old state and erase newer facts;
- concurrency overlap is not required for stale-state corruption.

## Limitations

P17 does not establish:
- real DB snapshot/isolation behavior;
- optimistic-locking semantics;
- version-conflict API behavior;
- multi-JVM behavior;
- how long a snapshot may safely remain valid;
- final business policy for modification-after-payment/payment-after-cancellation.

## Forward boundary

Do not add versioning/locking yet.

P18 next studies process crash, transient state and durability non-guarantees.

Cluster 1.2 later introduces real persistence where stale-write protection can be selected and validated.
