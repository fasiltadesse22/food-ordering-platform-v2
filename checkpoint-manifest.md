# C1.2-P18 — Checkpoint Manifest (candidate pending exact CI)

Inherited verified checkpoint C1.2-P17 SHA 40144edeb245c9739e74aaabcf9e5359d6601ebf.
Authoritative Master Roadmap V8.0 and derived Cluster1.2 Part 1.2.18.
Real production evolution: bounded stale-CAS fresh-state re-evaluation for current local lifecycle operations in OrderWorkflowService (max2 attempts); payment/refund/local line modifications no auto retry; PostgresOrderRepository pre-COMMIT concurrency SQLState taxonomy (40001/40P01/55P03) distinct from P15 unknown COMMIT outcome. New typed OrderDatabaseConcurrencyException. No new schema/microservices/provider calls.
New real PostgreSQL Testcontainers cases: compatible milestone+acceptance, incompatible reject+acceptance, two-conflict budget, no payment retry, no stale line replacement replay, committed-but-unacknowledged save no re-execution, actual PG 55P03 and actual PG 40001. Inherited P17 raw-conflict service test now expects P18 re-evaluated ILLEGAL_TRANSITION; original P17 frozen checkpoint preserves before behavior.
Root Maven, P18 in-memory JVM SIGKILL negative and P12 PostgreSQL SIGKILL durability positive must PASS at same final SHA. Working branch is work/C1.2-P18; create checkpoints/C1.2-P18 ONLY after CI passes, verify frozen CI and exact SHA. Never change main.
Scope limits: no durable idempotency key/result table; no external payment settlement, refund, provider retries; no blind lost-COMMIT replay; no blanket retry of 40001/40P01/55P03; no measured contention/latency SLO; these require future evidence.
Next P19: aggregate sizing, contention and version conflict rates. Preserve P18 limited local-only retry scope across future external integration changes.
