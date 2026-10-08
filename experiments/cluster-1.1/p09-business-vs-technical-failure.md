# C1.1-P09 Experiment — Business Rejection Versus Technical Failure

## Question

Can the application and HTTP boundaries distinguish a correctly rejected business operation from a failure of the execution mechanism?

## Hypothesis

If P09 semantics are correct:

1. illegal lifecycle request returns OrderActionResult.Rejected rather than escaping as an application failure;
2. ownership violation returns Rejected;
3. unknown Order returns Rejected(ORDER_NOT_FOUND);
4. repository read/write failures propagate as technical exceptions and are not converted to Rejected;
5. HTTP repeated cancellation returns 409 with ILLEGAL_TRANSITION;
6. HTTP wrong-owner cancellation returns 403;
7. HTTP unknown Order cancellation returns 404;
8. unexpected programming/runtime failure maps to 500, not 400.

## Controlled variables

Business-rejection tests keep repository mechanism healthy and vary business state/context.

Technical-failure tests keep the requested business action valid while replacing repository behavior with deterministic read/write failure.

This isolates:
- business decision failure;
from
- mechanism execution failure.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P09 CI.

## Limitations

This experiment does not determine:
- whether a technical failure is retryable;
- retry/backoff policy;
- circuit-breaker behavior;
- dependency timeout policy;
- final platform-wide error envelope;
- distributed/network failure semantics.

## Conclusion

Pending execution evidence.
