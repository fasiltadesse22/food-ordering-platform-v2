# C1.3-P01 — Strategic domain discovery baseline checkpoint manifest

## Baseline and contract
Previous verified frozen checkpoint: `checkpoints/C1.2-FINAL`, SHA `65139ae1fc33df685b2e651539d500626589cb55`.
Inherited C1.2 FINAL manifest remains available unchanged on its frozen branch.
Scope: V8.0 Cluster 1.3, derived blueprint, Part 1.3.1 only.
Engineering question: Why is a correct Order aggregate insufficient to establish the semantic architecture of the overall business?
Type A + C; no production-code, API, schema or topology changes.

## Predicted versus verified
Prediction before change: caller-supplied valid OrderLine prices are accepted without any authoritative Catalog lookup, correct RestaurantId allows local acceptance, and recordPayment changes only local Order facts. Observed source structure supports these hypotheses. New P01 CI test results are NOT assumed; verification is required on exact commit.

## Added
`applications/food-ordering-app/src/test/java/com/acme/foodordering/application/semantics/Cluster13SemanticBaselineTest.java`
`architecture/cluster-1.3/C1.3-P01-strategic-domain-baseline.md`
`architecture/scenarios/C1.3-P01-capability-and-authority-matrix.md`
`experiments/cluster-1.3/p01-semantic-authority-experiment.md`
`architecture/evidence/C1.3-P01-evidence.md`

## Actual testing gate (do not claim success until observed)
GitHub verify.yml: `mvn -B -ntp verify`, P18 in-memory process-restart control, P12 PostgreSQL SIGKILL/fresh JVM durability control.
Focused new test `Cluster13SemanticBaselineTest`. CI result and same-SHA verification must be reported externally once observed; frozen branch may only be created after green CI.

## Business/architectural decision
Keep the single real Spring Boot deployable and frozen Order aggregate architecture. The result is a source-grounded responsibility inventory, not declared bounded contexts. No Catalog business policy, Restaurant operating model, Inventory authority or Payment provider was manufactured.

## Preserved fragilities and deferred mechanisms
Price comes from current placement request; no authoritative Catalog validation. Restaurant acceptance has local ID guard only. Local PAYMENT_RECORDED is not provider truth; Inventory plan is not stock admission. Preserve as evidence for future evolution. No Kafka, Redis, Saga, Outbox, separate deployables/databases, or artificial ACL at P01.

## Next handoff
Part 1.3.2: classify business subdomains and problem-space capabilities without presupposing a bounded-context/service map; inherit this P01 exact verified SHA if CI passes.
