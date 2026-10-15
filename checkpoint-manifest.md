# Checkpoint Manifest — C1.2-P13 Final Candidate

Part: 1.2.13 Relational Constraints and Defense-in-Depth Integrity.
Source: Course 2 V8.0 and derived Cluster 1.2 blueprint; preserve uniform depth.
Inherited verified P12: f28facdf54366f689a2a5d278864a8d8dcc754bb.
Implementation candidate: 2dc184df0fc1e50726ae814507b7862460abdb00; CI success 38694233940.
Final documentation/evidence commit is not considered frozen until its own exact CI and frozen-ref CI pass.

Changes:
- V2__add_order_local_integrity_constraints.sql: 11 PostgreSQL row-local checks, no rewrite of V1.
- Twelve new direct SQL Testcontainers integrity+dirty migration experiments.
- P11 schema test now V1+V2 expectations; prior negative invalid line case becomes positive 23514 check.
- P12 JDBC adapter test now confirms invalid qty rejected in SQL and original valid row unchanged.
- ADR-0032, scenario, experiment ledger, evidence, checkpoint manifest.
Preserved original P18 memory fail after restart and P12 positive PostgreSQL recovery.
Known deferred failures: invalid COMPLETED without history accepted by DB, missing child lines accepted by DB, read snapshot inconsistency, stale successor overwrite, non-idempotent placement retries, local payment != provider financial truth.
Do NOT introduce version/CAS, JPA mapping redesign, triggers duplicating lifecycle, microservices or extra business tables in this part.
Next P14: Persistence Mapping, Aggregate Reconstitution and Representation Trade-offs; inherit frozen C1.2-P13 SHA not main.
