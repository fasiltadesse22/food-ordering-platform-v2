# C1.1-P16 Experiment — Duplicate Payment and External Effect

## Question

Can repeated payment execution create duplicate local facts and duplicate independently committed external effects, and can failure ordering make local and external payment truths diverge?

## Execution

GitHub Actions run:
38032714055

Command:

mvn -B -ntp verify

## Observed test result

OrderDuplicatePaymentExternalEffectTest:
- tests run: 4
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 82
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Repeated local payment — observed

Two recordPayment calls:
- first returned Accepted;
- second returned Accepted.

Final current Order:
- PAYMENT_RECORDED count = 2.

Interpretation:
current local payment milestone has no duplicate guard.

## Charge then record repeated twice — observed

Two complete coordinator executions:
- external committed charges = 2;
- local PAYMENT_RECORDED count = 2.

Interpretation:
without logical payment identity/idempotency, repeated execution duplicates both modeled external effect and local fact.

## External charge commits, response lost, then retry — observed

First attempt:
- modeled external authority committed charge #1;
- response was then lost by the test authority;
- coordinator aborted before local recordPayment;
- local PAYMENT_RECORDED count remained 0.

Retry:
- external authority committed charge #2;
- response returned;
- local recordPayment succeeded once.

Final:
- external committed charges = 2;
- local PAYMENT_RECORDED count = 1.

Interpretation:
the first provider failure seen by the application was an unknown outcome, not proof of no charge.

Blind retry multiplied the external effect.

## Record then charge, provider fails before effect — observed

Local recordPayment:
Accepted.

Provider:
failed before charge.

Final:
- external committed charges = 0;
- local PAYMENT_RECORDED count = 1.

Interpretation:
reversing call order creates the opposite inconsistency window.

## Core evidence

Executed and verified:
- duplicate local payment facts are currently permitted;
- naive repeated external payment execution can charge twice;
- provider response loss after external commit can produce external=2/local=1 after retry;
- local-first ordering can produce external=0/local=1.

Evidence-backed inference:
there is no safe sequential ordering of two independently committed authorities that makes them atomic.

The inconsistency window moves when ordering changes.

## Limitations

The modeled PaymentAuthority is a deterministic test double.

Not established:
- real provider network semantics;
- provider-side idempotency;
- durable provider transaction identity;
- distributed transaction guarantees;
- reconciliation;
- refund/compensation correctness;
- retry policy under real latency/failure.

## Forward boundary

P16 does not introduce the full idempotency solution.

P17 next studies stale-state decisions and temporal correctness.
