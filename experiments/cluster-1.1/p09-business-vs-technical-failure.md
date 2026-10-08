# C1.1-P09 Experiment — Business Rejection Versus Technical Failure

## Question

Can the application and HTTP boundaries distinguish a correctly rejected business operation from a failure of the execution mechanism?

## Execution

GitHub Actions run: 37785102920

Command:

mvn -B -ntp verify

## Observation

OrderActionFailureSemanticsTest:
- tests run: 5
- failures: 0
- errors: 0
- skipped: 0

ApiExceptionHandlerTest:
- tests run: 1
- failures: 0
- errors: 0
- skipped: 0

OrderHttpIntegrationTest:
- tests run: 5
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 53
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Evidence

Observed application behavior:
- illegal lifecycle request returns Rejected(ILLEGAL_TRANSITION);
- ownership violation returns Rejected(CUSTOMER_DOES_NOT_OWN_ORDER);
- unknown Order returns Rejected(ORDER_NOT_FOUND);
- repository read failure propagates IllegalStateException("repository unavailable");
- repository write failure propagates IllegalStateException("repository write failed").

Observed HTTP behavior:
- valid cancellation returns 200;
- repeated cancellation returns 409 and ILLEGAL_TRANSITION;
- wrong-customer cancellation returns 403 and CUSTOMER_DOES_NOT_OWN_ORDER;
- unknown Order cancellation returns 404 and ORDER_NOT_FOUND;
- unexpected NullPointerException maps to 500 with generic technical-failure detail.

## Interpretation

Expected business rejection is now part of the normal application result contract.

Technical failure remains exceptional execution and is not disguised as business rejection.

The same domain rules from earlier checkpoints remain enforced.

## Limitations

The experiment does not determine:
- whether technical failure is retryable;
- retry/backoff policy;
- timeout policy;
- circuit-breaker policy;
- distributed dependency failure semantics;
- final platform-wide error envelope.

## Conclusion

P09 successfully separates business rejection from technical failure without introducing resilience mechanisms.
