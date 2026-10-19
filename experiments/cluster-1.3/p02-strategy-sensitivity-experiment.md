# C1.3-P02 — Controlled Strategy-Sensitivity Classification Experiment

**Type:** qualitative business/architecture reasoning experiment; NOT runtime, load, financial or user research.  
**Application baseline:** `checkpoints/C1.3-P01` `c369d44fbdea5cead7920cfb13e9304707692a07`.  
**Held constant:** existing source behavior and the P01 capability/authority inventory.

## Question
Does a change in assumed competitive strategy change the proper core/supporting/generic classification while local source, business nouns, current guarantees and candidate capabilities stay the same?

## Hypothesis and prediction
H1: If differentiated value comes from fulfilled-when-accepted orders at peak demand, Restaurant Operations admission and capacity-aware promise policies are **core candidates**; ordinary payment transport remains a generic-but-critical candidate.
H2: If merchant financial trust drives differentiation, reconciliation/uncertain-outcome policy becomes a **core candidate**, while raw gateway transport may remain generic and Restaurant Operations may be supporting.
H3: The same broad Payment label can contain commodity and distinct business-policy subproblems. Hence one label for the entire technical subsystem can conceal strategic variation.

## Setup and controlled variable
Fixed candidate inventory: Ordering placement/lifecycle; sellable offering/Catalog quote; Restaurant readiness; inventory admission; financial authorization rails and merchant reconciliation; Customer accounts; Delivery; Notification.
Fixed source: real PlaceOrderService, Order, OrderWorkflowService, OrderInventoryReservationPolicy and P01 authority evidence.
Variable: *only* the hypothetical market/customer promise A (operational promise accuracy) versus B (merchant financial trust).
Assume no verified company objectives, interviews or revenue/retention data; any classification is conditional.

## Execution steps
1. For A and B, state the chosen customer segment, claimed reason for choosing the product and credible alternatives.
2. Classify each candidate into core/supporting/generic/unknown at the subproblem granularity necessary to avoid collapsing distinct rules.
3. Record reasoning chain: assumed unique promise -> necessary domain knowledge -> investment priority -> expected software/quality consequence.
4. Compare the classification table; highlight what changes (Restaurant admission, merchant reconciliation), what may remain generic (commodity payment rails), and what remains unknown.
5. Try to falsify A and B with contrary stakeholder/customer evidence and an available standard vendor solution.
6. Re-explain why local Order consistency is not proof of either strategy or its absent operational/financial authority.

## Conceptual observation from this analysis
Under the stated assumptions, A and B produce **different candidate classifications** without any change to the Java source. This follows from the declared strategy criteria, not from empirically observing marketplace adoption. Raw payment rail remains a plausible generic primitive in both; merchant-facing reconciliation can become core in B.

## Evidence and uncertainty
**SOURCE OBSERVATION:** P01 classes and authority matrix remain unchanged; Order has local lifecycle and payment milestone, not provider settlement or operating capacity admission.
**ILLUSTRATIVE RESULT:** A/B classification differs as the assumed strategy changes.
**NOT OBSERVED:** real merchant preference, refusal rates, finance failure rates, business leadership decision, service latency, production throughput or market impact.
**NO AUTONOMOUS TEST RUN:** a workshop matrix cannot be asserted as a passing Maven experiment.
**NON-GUARANTEE:** a classification category is not an independent runtime boundary, a database schema, a security guarantee or an SLO.

## Conclusion and falsification
The classification is **strategy-sensitive** and remains provisional. A's core Restaurant admission hypothesis fails if reliable fulfillment is not a differentiated buying reason or if a standard vendor adequately delivers it. B's core reconciliation hypothesis fails if merchant trust is not the reason merchants choose the platform or if available commodity solutions fully deliver the supposedly distinctive policy.

## Deferred business/technical requirements
No new external authority, integration contracts, Saga/Kafka/Redis, independently deployed services or payment execution code is justified solely by this reasoning exercise. For later implementation, request an approved business policy, observable acceptance tests and clear data/fact authority first.
