# C1.2-P08 — Lifecycle References and Service Boundary Experiments

Inherited verified baseline: `670ce14c7f985015dc65c3caafc60113a97d7c90`, checkpoint C1.2-P07.
Eight deterministic tests in OrderAggregateLifecycleReferenceBoundaryTest.
E1 same CustomerId/RestaurantId, distinct OrderIds -> one Order accepts; other remains PLACED; independent lifecycles.
E2 Order normal evolution preserves both cross-aggregate ID references and its own identity/placedAt, without proving external entity existence.
E3 application service loads repository Order, delegates acceptance to root, saves successor and projects OrderSnapshot; previous immutable representation stays PLACED.
E4 nonmatching acting RestaurantId rejected through application path with no authority replacement; not proof of authentication.
E5 mismatched supplied ActorContext on placement rejected before current Order publication; ActorContext remains unverified application input.
E6 accepted placement yields OrderStored state and OrderPlaced local fact for the same identity, not an integration event/broker delivery.
E7 local recordPayment adds PAYMENT_RECORDED to Order while lifecycle remains PLACED; no external provider authority exists in the implementation. This is a negative scope control.
E8 cancelled terminal-status Order with locally recorded payment may request refund, preserving OrderId and status; no provider refund.
E9 architecture-only adversarial thought experiment: two Orders for one Restaurant capacity slot, both evaluate stale snapshot capacity=1 then accept; no actual RestaurantCapacity model/experiment in P08, no false claims.
E10 inherited: P13/P14/P17 stale/conflict failures, P16 external divergence, P18 SIGKILL restart loss.
Verification: exact P08 commit root mvn -B -ntp verify + existing P18 JVM process restart script in GitHub Actions; checkpoint only after success.
