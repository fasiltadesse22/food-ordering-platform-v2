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

Authoritative execution command:

```bash
mvn -B -ntp verify
```

## Observation

**Pending until the P02 GitHub Actions run completes.**

Do not replace this line with PASS based on source inspection.

## Evidence

Pending CI run identifier and test output.

## Interpretation

If the prediction is observed, the application boundary structurally represents actor/use-case responsibility rather than leaving it only in documentation or HTTP routing.

## Limitations

Even a passing test does not prove:

- real caller authentication;
- a complete authorization model;
- restaurant staff identity semantics;
- payment authority;
- distributed security;
- lifecycle correctness beyond Place Order.

## Conclusion

Pending execution evidence.
