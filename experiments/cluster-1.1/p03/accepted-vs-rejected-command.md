# C1.1-P03 Experiment — Accepted vs Rejected Command

## Question

Does the application make a visible semantic distinction between a requested command and an accepted business fact?

## Hypothesis

If command and fact semantics are modeled correctly, an accepted PlaceOrder command will produce current order state plus an OrderPlaced fact, while a rejected PlaceOrder command will produce neither persisted order state nor OrderPlaced fact.

## Prediction

Accepted path:
- customer actor customer-1 requests PlaceOrder for customer-1;
- result type is PlaceOrderResult.Accepted;
- repository size becomes 1;
- accepted result contains OrderPlaced;
- fact identity/time/total correspond to the resulting order.

Rejected path:
- restaurant operator submits the same customer PlaceOrder instruction;
- result type is PlaceOrderResult.Rejected;
- repository size remains 0;
- result type contains a rejection, not an OrderPlaced fact.

## Setup

Executable test:

applications/food-ordering-app/src/test/java/
com/acme/foodordering/application/semantics/PlaceOrderSemanticFlowTest.java

Dependencies:
- PlaceOrderService;
- InMemoryOrderRepository;
- fixed Clock;
- ActorContext;
- PlaceOrderCommand.

## Controlled variable

Actor context is changed while the requested order content remains equivalent.

## Execution

GitHub Actions run: 37632251569

Command:

mvn -B -ntp verify

## Observation

PlaceOrderSemanticFlowTest:
- tests run: 2
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 11
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

Observed behavior matched the prediction.

## Evidence

Accepted path:
- result was PlaceOrderResult.Accepted;
- repository contained one order;
- accepted result contained OrderPlaced;
- fact order identity and occurrence time matched the resulting order.

Rejected path:
- result was PlaceOrderResult.Rejected;
- rejection code was ACTOR_TYPE_NOT_ALLOWED;
- repository remained empty;
- Rejected type has no OrderPlaced fact.

Inherited P01/P02 tests remained green.

## Interpretation

Receipt of a command is not evidence that the requested business occurrence happened.

The application now exposes an explicit decision outcome. Only the accepted path produces OrderPlaced.

## Limitations

The experiment does not establish:
- durable event history;
- event publication;
- cross-process propagation;
- exactly-once behavior;
- retry or duplicate semantics;
- lifecycle legality for future commands;
- final business/technical failure taxonomy.

## Conclusion

The controlled P03 execution supports the semantic distinction:

request/command != accepted fact.
