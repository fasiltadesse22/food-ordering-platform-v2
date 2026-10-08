# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P08
Current evolution: C1.1-P09 candidate — Business Failure Versus Technical Failure

## Verification

mvn -B -ntp verify

P09 is not frozen until GitHub Actions verifies the exact candidate.

## Failure semantics

Expected business outcome:
- Accepted
- Rejected with stable business code

Examples:
- illegal transition;
- wrong owner;
- refund/modification rule violation;
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
