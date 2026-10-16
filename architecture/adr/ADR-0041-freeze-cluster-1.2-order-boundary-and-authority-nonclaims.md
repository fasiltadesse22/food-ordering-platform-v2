# ADR-0041 — Freeze Cluster 1.2 Order Consistency Contract and Separate Unimplemented External Authorities

Status: Accepted final Cluster1.2 architecture decision, verification evidence via exact GitHub CI on C1.2-P22 work and C1.2-FINAL branches.
Authority: Master Roadmap V8.0 and derived Cluster1.2 22-part blueprint; inherited frozen C1.2-P21 SHA c1bfd5a3d11b0bcc000da33b77bd7f90a3970b6c.

## Context
Parts 1.2.1-1.2.21 established one immutable Order root, typed value identity, selected lifecycle/workflow invariants, conceptual persistence, PostgreSQL Flyway V1-V3, atomic root/lines/history local commit, coherent read snapshot, optimistic version CAS, bounded select retry, SQLState/commit ambiguity distinction, full-child persistence write amplification, split-commit failure controls, and pure inventory planning with test-only local stock transaction. P22 must synthesize this evidence, not preempt Cluster1.3 or introduce unsupported business authority.

## Decision
1. Retain single Spring Boot deployable, Order aggregate root, typed CustomerId/RestaurantId references, owned OrderLine and selected workflow facts, versioned same-local-DB transaction.
2. Reject independently committed Order status/history under current business implications. Do not conflate physical child tables with aggregates.
3. Preserve P15 unknown COMMIT outcome, P16 snapshot and P17 stale-version CAS, P18 operation-specific conflict policy rather than catch-all retries.
4. Preserve P19 observed full-child-row churn as a named measured cost. A future append-oriented SQL optimization can retain one logical Order aggregate; its adoption requires performance and correctness revalidation.
5. Keep P21 pure inventory reservation proposal as non-authoritative. Current production PlaceOrderService remains stock-unaware; no claim of strong at-placement inventory guarantee. Experimental same-DB coordinator, stock tables, trigger failpoints are test-only.
6. Do not claim Restaurant operational, Payment provider capture/refund, delivery, durable request idempotency, outbox, Saga, CDC, Kafka, Redis or service/database per aggregate implementation.
7. New five-test P22 Testcontainers final integrated contract confirms actual Order identity/history persistence, stale currentness, Restaurant owner guard, invalid SQL cross-table status/history rejection, and stock PLAN non-admission. Preserve 234 inherited tests and JVM restart scripts. No change to production behavior in P22.

## Alternatives
A Split Order status/history into independently committed aggregates: P20 real DB invalid committed states, rejected.
B Merge entire Restaurant/Payment/Inventory with Order: independently authoritative truth absent, false ownership, rejected.
C Independently deploy every Java domain package: arbitrary network boundaries and failure modes, rejected.
D Same local PostgreSQL for justified multiple aggregates in a selected business transaction: demonstrated P21 test, valid when contract defines strong same-DB operation; not wired to product without lifecycle.
E Eventual cross-system workflow: potentially valid when participant independence/business temporal contract justifies, not selected in Cluster1.2.
F Optimize current physical history/line persistence: supported candidate from P19, requires representative latency/WAL/lock testing and invariant/atomicity regression.
G Leave current verified simple mechanism and document technical debt: chosen for cluster freeze.

## Tradeoffs and nonclaims
Current saveCurrent performs full-child replacement; can generate substantial row churn and root lock contention for a hot Order. JDBC uses DriverManager per operation, not an established production connection pool. Current P21 stock policy only advisory; database authority exists only in tests. Selected OrderInvariants.verify implications are not comprehensive event-history validation. Local PAYMENT_RECORDED fact cannot authorize claims of provider settlement. Client HTTP timeouts and unknown COMMIT cannot be reconciled generally without stable operation identity. These remain explicit risk items.

## Handoff
Only verified frozen C1.2-FINAL commit is authoritative baseline for Cluster1.3 project changes. Cluster1.3 is bounded contexts, ubiquitous language, context relationships, translations and semantic ownership; it must inherit, not silently rewrite, the proven Cluster1.2 consistency contract. Different contexts may share IDs without sharing models, databases, teams or deployments.
