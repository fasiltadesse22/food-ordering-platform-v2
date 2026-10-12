# Checkpoint Manifest — C1.2-P09 Candidate

Part 1.2.9 — Domain Facts, Domain Events and Aggregate Decisions.
Inherited verified base checkpoints/C1.2-P08 5f78c68fc04b76f34fbc431010a677c0ba83bc9d.
Evolution B/C/A. Real production-domain change: OrderPlaced.from initial-placement-state guard. Nine executable tests and deep architecture/evidence artifacts.
Files:
- applications/food-ordering-app/src/main/java/com/acme/foodordering/domain/order/fact/OrderPlaced.java
- applications/food-ordering-app/src/test/java/com/acme/foodordering/application/semantics/OrderDomainFactDecisionBoundaryTest.java
- architecture/scenarios/C1.2-P09-domain-facts-events-and-decisions.md
- architecture/adr/ADR-0028-protect-placement-fact-semantics-defer-events.md
- experiments/cluster-1.2/p09-domain-fact-and-event-semantics.md
- architecture/evidence/C1.2-P09-evidence.md
- checkpoint-manifest.md

Contract: Fact factory can no longer reconstruct historical placement total from later modified or workflow-evolved Order snapshot. App creates OrderPlaced only after current save returns. Never claim public record constructor proves committed origin.
Deferred: P10 repository identity and authority; P11–P15 database/transaction; P16–P18 concurrency/retry; later messaging, outbox, CDC and event sourcing only if justified.
Preserved fragilities: P13/P14 conflicting accepted decisions; P15 repeated commands, P16 provider divergence, P17 stale overwrite; P18 process loss.
Verification: exact-commit CI Maven root verify + P18 real JVM restart before frozen branch. Candidate is not verified until those succeed.
Next P1.2.10 Repository Semantics, Persistence Authority and Reconstitution.
