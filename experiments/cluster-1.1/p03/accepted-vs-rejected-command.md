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

Authoritative command:

mvn -B -ntp verify

## Observation

Pending the P03 GitHub Actions run.

## Evidence

Pending CI run identifier and test output.

## Interpretation

If observed, the project demonstrates that receipt of a command is not itself evidence that the corresponding business fact occurred.

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

Pending execution evidence.
