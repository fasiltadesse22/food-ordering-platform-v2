# ADR-0009 — Separate Expected Business Rejection from Technical Failure

Status: Accepted for C1.1-P09 candidate

## Context

Before P09, the domain correctly rejected illegal transitions and failed guards through exceptions such as:

- IllegalOrderTransitionException
- OrderGuardViolationException

Infrastructure/runtime failures also surface through exceptions.

At the Java mechanism level this made two semantically different outcomes look similar:

1. the system successfully evaluated a request and correctly decided "no";
2. the system could not execute correctly because software/infrastructure failed.

The HTTP exception handler also mapped NullPointerException to HTTP 400, which incorrectly classified a programming defect as a client/business problem.

## Decision

Introduce an explicit application result for Order actions:

OrderActionResult
- Accepted(OrderSnapshot)
- Rejected(OrderActionRejection)

Expected domain rejections are translated at the application boundary into Rejected.

Known rejection codes include:
- ORDER_NOT_FOUND
- ILLEGAL_TRANSITION
- CUSTOMER_DOES_NOT_OWN_ORDER
- RESTAURANT_DOES_NOT_OWN_ORDER
- REFUND_REQUIRES_RECORDED_PAYMENT
- REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER
- REFUND_ALREADY_REQUESTED
- MODIFICATION_REQUIRES_PLACED_ORDER

Technical/runtime failures are not converted to OrderActionResult.Rejected.

Examples:
- repository read failure;
- repository write failure;
- unexpected programming exception.

They propagate to the technical-failure boundary.

## HTTP mapping

For the new cancellation HTTP path:

- accepted cancellation -> 200;
- unknown Order -> 404 with stable business code;
- ownership rejection -> 403 with stable business code;
- lifecycle/business conflict -> 409 with stable business code;
- unexpected technical exception -> 500 with generic technical-failure response.

IllegalArgumentException from malformed request/value construction remains 400.

NullPointerException is no longer treated as a bad request.

## Why exceptions remain inside the domain

Domain exceptions remain useful enforcement mechanics.

The distinction is made at the application boundary:

domain mechanism:
throw when a domain rule is violated

application semantic:
convert known expected domain violations into an explicit rejected business outcome

Unexpected exceptions are not caught generically.

## Why no retry/circuit breaker

P09 classifies failure.

It does not yet solve technical failure through resilience mechanisms.

A repository failure is intentionally left visible as a technical failure.

Retry, timeout and circuit-breaker policy require later evidence about failure mode, idempotency and dependency behavior.

## Consequences

Positive:
- expected business rejection becomes part of the normal application contract;
- callers do not need to interpret every exception as system malfunction;
- stable rejection codes support HTTP/API behavior and future observability;
- technical failures remain distinguishable for reliability/incident handling.

Negative:
- application services must translate domain rejection mechanics;
- result handling adds explicit branching;
- transport mappings still require policy decisions.

## Important scope limits

P09 does not establish:
- final public API error standard for the whole platform;
- retryability taxonomy;
- distributed dependency failures;
- logging/metrics/tracing policy;
- circuit breakers;
- idempotency;
- persistence.

## Evolution

P10 will distinguish invariants, validation rules and database constraints.

Later resilience clusters will decide what to do about technical failure after P09 has established what kind of failure occurred.
