# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P09 — Business Failure Versus Technical Failure
Previous checkpoint: checkpoints/C1.1-P08

## Verification

mvn -B -ntp verify

P09 implementation verification:

Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Failure semantics

Expected business outcome:
- Accepted
- Rejected with stable business code

Verified examples:
- illegal transition;
- wrong owner;
- unknown Order.

Technical failure:
- repository/runtime/programming failure that prevents correct execution.

Technical failures are not converted to business rejection.

## HTTP example

POST /orders/{orderId}/cancel

- 200 accepted
- 403 ownership rejection
- 404 unknown Order
- 409 lifecycle conflict
- 500 unexpected technical failure

## Important boundary

P09 classifies failure. It does not add retry, timeout, fallback or circuit breaker mechanisms.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
