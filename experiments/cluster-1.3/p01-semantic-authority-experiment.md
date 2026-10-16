# C1.3-P01 — Current source-semantic authority negative controls

## Engineering question
Does correct current Order placement establish that its commercial line price was authorized by a Catalog model? What does its Restaurant acceptance and payment milestone establish?

## Hypothesis and prediction
Hypothesis H1: current PlaceOrderService has no Catalog authority lookup. Prediction: two structurally valid orders with same item ID and different positive caller-supplied prices are both accepted and persist those values in the selected in-memory repository.
H2: RestaurantId check is a local guard only. Prediction: matching ID permits acceptance from PLACED; mismatched ID is rejected.
H3: PAYMENT_RECORDED is a local Order milestone. Prediction: recordPayment leaves OrderStatus PLACED and appends one local occurrence.

## Setup and controlled variables
New `Cluster13SemanticBaselineTest` uses the actual production Order/PlaceOrderService implementation, InMemoryOrderRepository, fixed Clock, and stable item/restaurant/customer identifiers. Only unitPrice is varied in H1. H2 varies acting RestaurantId; H3 invokes local recordPayment. No Catalog/Payment provider, no fake external price or settlement table, and no HTTP/network assumptions are introduced.

## Reproducible commands
`mvn -B -ntp -pl applications/food-ordering-app -Dtest=Cluster13SemanticBaselineTest test`
`mvn -B -ntp verify`
The root CI also includes inherited JVM restart durability scripts. Commands are verification requirements, not an assertion they ran until CI confirms.

## Interpretation limitations
H1 can only establish the current service's acceptance/persistence behavior for provided line values. It does NOT establish that either price is correct, nor assert a security exploit without a specified commercial pricing requirement. H2 does NOT simulate operational Restaurant authority. H3 does NOT demonstrate provider failure or financial settlement. Memory adapter is intentional: it isolates semantic behavior; inherited PostgreSQL tests prove separate persistence guarantees.

## Failure diagnostic protocol
Symptom: customer reports wrong Order price.
Scope: which Order(s), actor/request and item references?
Timeline: request -> service -> Order.place -> repository.createNew.
Evidence: request fields, accepted Order snapshot, actual source dependency, prior catalog contract (if any).
Hypotheses: caller price accepted by design; unauthorized amount accepted against future policy; source Catalog stale; incorrect translation.
Discriminating test: compare recorded request/OrderLine and applicable authoritative business price if later implemented; at P01 no such authority exists.
Mechanism: current placement copies accepted input into OrderLine.
Mitigation: establish real business pricing authority before changing API, never pretend an ACL or cache fixes a missing business contract.
Validation: introduce an authorized price policy and regression tests only in a later justified part.

## Before/after evidence
Before state preserved as `checkpoints/C1.2-FINAL` SHA 65139ae1fc33df685b2e651539d500626589cb55.
P01 introduces only source-grounded docs and a dedicated test class. No production code change; do not claim an architecture improvement beyond improved evidence at P01.
