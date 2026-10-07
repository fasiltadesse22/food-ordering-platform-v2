# C1.1-P02 Experiment — Invalid Actor Attempts

## Question

Does the Place Order application boundary distinguish the actor who is allowed to execute the use case from other business participants?

## Hypothesis

If actor responsibility is modeled explicitly, a customer acting for themself should be accepted while a restaurant operator or a mismatched customer identity should be rejected before order persistence.

## Prediction

1. Customer `customer-1` placing for `customer-1` → success and one persisted order.
2. Restaurant operator attempting Place Order → `ActorNotAllowedException` and zero persisted orders.
3. Customer `customer-2` placing for `customer-1` → `ActorNotAllowedException` and zero persisted orders.

## Setup

Executable acceptance test:

```text
applications/food-ordering-app/src/test/java/
  com/acme/foodordering/application/acceptance/
  PlaceOrderActorAcceptanceTest.java
```

Dependencies:

- `PlaceOrderService`;
- in-memory repository;
- fixed `Clock`;
- explicit `ActorContext`.

## Controlled variable

Actor context is changed while the order payload, restaurant and order line remain equivalent.

## Execution

Authoritative command executed by GitHub Actions:

```bash
mvn -B -ntp verify
```

Workflow run:

```text
37629067834
```

## Observation

```text
PlaceOrderActorAcceptanceTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Overall reactor:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Observed behavior matched all three predictions.

## Evidence

- valid customer actor test: passed;
- restaurant-operator invalid attempt: rejected and repository remained empty;
- mismatched-customer invalid attempt: rejected and repository remained empty;
- inherited P01 tests remained green.

## Interpretation

The Place Order application boundary now represents business actor/use-case responsibility explicitly rather than relying only on HTTP routing or documentation.

The result demonstrates **use-case eligibility under the modeled ActorContext**, not real-world identity proof.

## Limitations

The passing experiment does not establish:

- authentication;
- a complete authorization model;
- restaurant staff identity semantics;
- payment authority;
- distributed security;
- lifecycle correctness beyond Place Order.

## Conclusion

The prediction was supported in the controlled P02 test environment. Actor/use-case responsibility is now executable evidence in the project while security infrastructure and service decomposition remain intentionally absent.
