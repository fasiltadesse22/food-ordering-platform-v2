# Checkpoint Manifest — C1.1-P09 Candidate

## Identity

- Target checkpoint: C1.1-P09
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P08
- Inherited branch: checkpoints/C1.1-P08
- Inherited commit: c6e6d78f42e5e3c47125adaffa6f675e6207b54f

## Engineering question

When an operation does not produce the requested outcome, did the system correctly reject the business request, or did the system itself fail to execute correctly?

## Application semantics

Expected business outcomes:
- Accepted(OrderSnapshot)
- Rejected(OrderActionRejection)

Known expected domain violations are translated to Rejected.

Technical runtime/infrastructure failures are not translated into business rejection.

## HTTP evidence target

- valid cancel -> 200
- repeat cancel -> 409 / ILLEGAL_TRANSITION
- wrong customer -> 403 / CUSTOMER_DOES_NOT_OWN_ORDER
- unknown order -> 404 / ORDER_NOT_FOUND
- unexpected runtime/programming failure -> 500

## Mechanisms deliberately absent

- retry;
- backoff;
- circuit breaker;
- fallback;
- PostgreSQL;
- Kafka;
- Redis;
- distributed failure handling.

## Evidence status

Pending P09 CI.

## Freeze gate

1. root Maven verification succeeds;
2. inherited P01-P08 behavior remains green under the explicit result contract;
3. known business violations return Rejected rather than technical failure;
4. repository read/write failure propagates as technical failure;
5. HTTP business rejection mappings execute as expected;
6. unexpected runtime failure maps to 500 rather than 400.
