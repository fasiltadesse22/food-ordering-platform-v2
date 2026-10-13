# C1.2-P12 — Durable PostgreSQL Recovery Experiments

## Experimental methodology
Question -> Hypothesis -> Prediction -> Setup -> Controlled variable -> Execution -> Observation -> Evidence -> Interpretation -> Limitations -> Conclusion.

### E1: Full Order reconstruction from durable PostgreSQL
Hypothesis: Independent PostgresOrderRepository instances recover same business OrderId and ordered lines. Setup real Testcontainers postgres16 + Flyway V1. Observation pass, different Java reference with same domain identity and values.

### E2: Persisted lifecycle and ordered workflow facts
Setup create Order, modify lines, record local payment, cancel, request local refund; commit each successor with JDBC; new adapter instance loads aggregate. Observation pass: CANCELLED, changed ordered lines, four exact occurrences and timestamps. Local payment occurrence is NOT proof of provider financial charge.

### E3: Identity collision
Second createNew with same UUID must throw OrderAlreadyExistsException without overwriting first. Observation pass.

### E4: Absent ID
No root -> Optional.empty. Observation pass, distinguished from SQL failures and corruption.

### E5: Malformed persisted status/history
Direct SQL changes status to COMPLETED without matching workflow history; read throws InvalidPersistedOrderException. Observation pass.

### E6: Invalid stored line value
Direct SQL changes line quantity to zero (P11 schema allows); domain reconstitution throws InvalidPersistedOrderException. Observation pass; P13 defense-in-depth gap preserved.

### E7: Stale successor negative control
Accept original PLACED, then overwrite with stale cancellation successor. Final state CANCELLED and only cancellation occurrence. Observation pass: durability != CAS.

### E8: Legacy upsert negative control
saveCurrent on absent ID still inserts (P10 behavior). Observation pass; update-only/versioning deferred.

### E9: Zero child lines corruption
Direct SQL deletes all lines; reconstruction refuses invalid Order. Observation pass.

### E10: Real two-JVM SIGKILL recovery
CI PostgreSQL service remains alive. Run executable Spring Boot JAR A, HTTP POST ->201 and cancel ->200. Kill process A with SIGKILL, start fresh JAR B same JDBC URL; GET same ID ->200 CANCELLED and same customer/restaurant/lines/total. Observation PASS in GitHub Actions run 38359212874. Script experiments/cluster-1.2/p12-postgresql-process-restart.sh.

### E11: Original memory process-loss before-state
Same workflow with default memory repository: process B GET ->404. Observation PASS as negative-control script C1.1-P18 in same run.

## CI run 38359212874
156 Maven tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS. Root reactor, P18 memory SIGKILL and P12 Postgres SIGKILL jobs all success. Details in architecture/evidence/C1.2-P12-evidence.md.

## Limitations
Process restart != PostgreSQL crash or disaster recovery. No currentness guarantee for reads under concurrent writers, no CAS/stale conflict, no provider charge, no independent event broker, no transaction commit ambiguity experiment. Reconstitution validates selected invariants, not all historical causal order.
