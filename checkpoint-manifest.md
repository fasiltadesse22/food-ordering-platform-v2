# Checkpoint Manifest — C1.2-P17 Final Candidate

Part: 1.2.17 Optimistic Concurrency, Versioning and Stale-Write Detection.
V8.0 and derived Cluster1.2 blueprint scope; continuous Food Ordering Platform.
Parent frozen verified C1.2-P16 SHA c48869ff7f9282229ce130ed8209fca0b78658bf.
Code+tests candidate ffc0fb28ca37999632bfe9d199eb120cac03675d, GitHub CI 38759021793 SUCCESS: Maven 204 tests all passed, new P17 PostgreSQL suite 10 passed, P18 memory negative and P12 PostgreSQL recovery positive.
This docs-bearing SHA remains candidate pending independent exact-commit CI and frozen branch CI.

Type B production evolution + Type C real PostgreSQL conflict/failure tests + Type A ADR/scenario/evidence.
Core files: immutable Order version; Flyway V3 schema; PostgresOrderRepository conditional SQL CAS/no upsert; PostgresOrderMapper revision reconstruction; InMemoryOrderRepository atomic compare; OrderRepository updated semantics; StaleOrderVersionException; OrderWorkflowService and ModifyOrderService explicit conflict result; HTTP409; new 10-test PostgresOrderOptimisticConcurrencyTest; inherited tests revised.
No message broker, new financial/provider authority, distributed lock, JPA rewrite, source event replay or automatic retries.

Observed winner-preservation, version progression, same-status conflicts, missing-order rejection, rollback of failed child write after version advanced, actual 204-test CI success. Trade-offs: whole Order version conflicts on any aggregate update; legacy writers must not bypass increment; no client supplied If-Match at HTTP; version is not operation identity, payment truth or idempotency. P18 is next: conflict resolution, transaction failure classification, retries and reconciliation. Inherit verified P17 frozen SHA, not main.
