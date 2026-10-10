# C1.1-P16 Experiment — Duplicate Payment and External Effect

## Question

Can repeated payment execution create duplicate local facts and duplicate independently committed external effects, and can failure ordering make local and external payment truths diverge?

## Hypothesis

Current recordPayment has no duplicate guard or logical command identity.

A naive external payment flow that combines a provider effect with local PAYMENT_RECORDED using ordinary sequential calls cannot make the two authorities atomic.

## Predictions

### Repeated local payment

Two recordPayment calls:
- both Accepted;
- current Order contains two PAYMENT_RECORDED occurrences.

### Charge then record, repeated

Two executions:
- external charges = 2;
- local PAYMENT_RECORDED = 2.

### External charge commits, response lost, then retry

First attempt:
- external charge #1 commits;
- provider response is lost;
- local PAYMENT_RECORDED is absent.

Retry:
- external charge #2 commits;
- local PAYMENT_RECORDED is recorded once.

Final:
- external charges = 2;
- local payment facts = 1.

### Record then charge, provider fails

Local PAYMENT_RECORDED succeeds.

External provider fails before charge.

Final:
- external charges = 0;
- local payment facts = 1.

## Setup

Use current real OrderWorkflowService.

Use test-only PaymentAuthority implementations solely as controlled independent effect authorities.

No production payment integration or idempotency mechanism is added.

## Controlled variables

The harness controls:
- whether external effect commits;
- whether response is returned;
- whether local recording happens before/after the external call;
- number of repeated attempts.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P16 CI.

## Evidence interpretation

If predictions hold:
- local PAYMENT_RECORDED must not be treated as proof of one external charge;
- repeated local state behavior cannot establish external-effect idempotency;
- unknown provider outcome makes blind retry unsafe;
- reversing call order changes which inconsistency is possible but does not create atomicity.

## Limitations

The PaymentAuthority is a deterministic test double.

P16 does not establish:
- real provider network behavior;
- provider-side idempotency semantics;
- durable payment identifiers;
- reconciliation;
- refund correctness;
- distributed transaction behavior.

## Forward boundary

Do not introduce the full later idempotency mechanism here.

Do not introduce Saga/Kafka/Outbox.

P17 next studies stale-state decisions.
