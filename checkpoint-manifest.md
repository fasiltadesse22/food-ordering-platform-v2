# Checkpoint Manifest — C1.1-P09

## Identity

- Checkpoint: C1.1-P09
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P08
- Inherited branch: checkpoints/C1.1-P08
- Inherited commit: c6e6d78f42e5e3c47125adaffa6f675e6207b54f
- P09 implementation commit: b232967485ee78f1cba56c26ca4b864dc07d10ea
- P09 verification run: 37785102920

## Engineering question

When an operation does not produce the requested outcome, did the system correctly reject the business request, or did the system itself fail to execute correctly?

## Verified application semantics

Expected business outcomes:
- Accepted(OrderSnapshot)
- Rejected(OrderActionRejection)

Verified rejection examples:
- ILLEGAL_TRANSITION
- CUSTOMER_DOES_NOT_OWN_ORDER
- ORDER_NOT_FOUND

Technical repository failures are not translated into Rejected.

## Verified HTTP semantics

- accepted cancel -> 200
- repeated cancel -> 409 / ILLEGAL_TRANSITION
- wrong customer -> 403 / CUSTOMER_DOES_NOT_OWN_ORDER
- unknown Order -> 404 / ORDER_NOT_FOUND
- unexpected programming/runtime failure -> 500

NullPointerException is no longer classified as a 400 request error.

## Verification

GitHub Actions executed:

mvn -B -ntp verify

Observed:

Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

## Architectural interpretation

P09 classifies failure before resilience mechanisms are introduced.

The project still contains no:
- retry/backoff;
- timeout policy;
- circuit breaker;
- fallback;
- PostgreSQL;
- Kafka;
- Redis;
- distributed failure coordination.

## Evidence-qualified guarantees

Within the tested one-process model:
- known expected domain rejection becomes an explicit application rejection;
- selected repository mechanism failures remain technical;
- selected HTTP adapter mappings preserve that distinction.

Not guaranteed:
- technical failure retryability;
- remote dependency behavior;
- durable persistence;
- platform-wide failure observability.

## Next pressure

Part 1.1.10 must distinguish business invariants, validation rules and database constraints and make the correctness rules explicit independently of their enforcement mechanism.
