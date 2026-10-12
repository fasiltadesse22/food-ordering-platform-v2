# ADR-0027 — Preserve Order-Local Behavior and Explicit Cross-Aggregate References

Status: Accepted for C1.2-P08 (scope-limited architectural decision).
Baseline: checkpoints/C1.2-P07 `670ce14c7f985015dc65c3caafc60113a97d7c90`.

## Context
P04 derived the provisional Order aggregate. P07 strengthened one status/history invariant.
The repository has only Order domain authority and in-process storage; CustomerId and RestaurantId identify related business concepts but not full owned aggregate state. PAYMENT_RECORDED is local Order history; no provider adapter exists.
The next design problem is which policies belong inside Order, an application service or a future independent domain service.

## Decision
Keep implemented Order-owned transition, modification and refund-eligibility rules in Order root.
Keep use-case orchestration and result projections in PlaceOrderService/OrderWorkflowService/ModifyOrderService.
Retain typed CustomerId and RestaurantId references; do not embed full Customer/Restaurant business entities.
Do not implement a domain service just to rename existing Order logic.
For any future Restaurant capacity/eligibility or Payment authority rule, derive the external authority, freshness, atomicity and conflict policy first. Only then consider a domain policy/service or coordinating application workflow.
Add focused tests proving independent Order lifecycles despite shared references and the actual service boundary. Keep payment fact/actor-context limitations explicit.

## Alternatives
A: Move root invariants into application services -> duplicated, bypassable domain rules; reject.
B: Order owns whole Restaurant, Customer, Payment -> false authority and unnecessary lifecycle coupling; reject.
C: Introduce RestaurantAcceptancePolicy now -> no implemented Restaurant facts or capacity invariant to test; defer.
D: Introduce Saga, Kafka, remote services, cross-aggregate transaction now -> no demonstrated need and violates roadmap staging; defer.
E: Current root + typed identity refs + application orchestration -> adequate for present requirements.

## Trade-offs / non-guarantees
Typed reference equality does not prove authenticated caller, remote entity existence, Restaurant open/capacity status or price validity.
Application service is not a domain service merely because its name ends in Service.
A hypothetical domain policy cannot guarantee atomic multi-authority commitment; check/act freshness failures remain possible.
Local Payment milestones do not guarantee charge/refund execution or once-only behavior.
No new production domain service or topology is introduced in P08.

## Revisit triggers
A documented immediate cross-aggregate invariant; actual Restaurant/Payment models and repositories; contention measurements; independent lifecycle needs; measured policy complexity; consistent and testable authority contracts.
