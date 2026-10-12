# Checkpoint Manifest — C1.2-P08 Candidate

Part: 1.2.8 Aggregate Lifecycle, References and Domain Services
Inherited verified checkpoint: checkpoints/C1.2-P07 (`670ce14c7f985015dc65c3caafc60113a97d7c90`).
Evolution: Type A/C, eight targeted boundary tests plus scenario, ADR, experiment and evidence ledgers. No production runtime changes.

Source-grounded decision:
Order owns local lifecycle, line modification and refund-eligibility decisions. CustomerId/RestaurantId are typed external identity references; complete Customer/Restaurant models not yet present.
PlaceOrderService/OrderWorkflowService orchestrate use cases; their Service name does not imply DDD domain service.
No evidence-based cross-aggregate policy justifies a new production domain service today.
Local OrderPlaced and PAYMENT_RECORDED are not broker integrations or provider financial proofs.
Status terminality is not the end of all local milestones.

Added files:
OrderAggregateLifecycleReferenceBoundaryTest (8 tests)
architecture/scenarios/C1.2-P08-aggregate-lifecycle-references-domain-services.md
architecture/adr/ADR-0027-order-lifecycle-references-and-domain-services.md
experiments/cluster-1.2/p08-lifecycle-reference-service-experiments.md
architecture/evidence/C1.2-P08-evidence.md
checkpoint-manifest.md

Verification:
GitHub Actions on exact evidence-bearing candidate SHA must pass Maven root reactor verify and P18 real JVM replacement script. Candidate status unverified before exact-CI result.
Preserve P13/P14/P15/P16/P17/P18 fragilities and P07 invariant.
No PostgreSQL, version field, transaction, domain-service abstraction, Restaurant/Payment aggregate, Saga, Kafka or topology split.

Next: Part 1.2.9 Domain Facts, Domain Events and Aggregate Decisions.
