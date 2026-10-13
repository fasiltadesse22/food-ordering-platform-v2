# Checkpoint Manifest — C1.2-P12 Final Candidate

Part 1.2.12 PostgreSQL Adapter, Reconstitution and Durable Recovery.
Source contract V8.0 roadmap + derived Cluster 1.2 blueprint.
Inherited C1.2-P11 frozen 1defeffc57170d86b5c0afc122f48b8f5ef826a2.
Implementation success SHA c39ee8b50d491b4e6f0f18333b32113ad6581628 CI 38359212874.
This final documentation child commit requires its own CI verification before checkpoint freeze.

Production implementation:
- PostgresOrderRepository JDBC adapter (three owned tables, create-only SQL root, legacy unconditional successor UPSERT, transactional full write, ordered reads).
- Order.reconstitute selected invariant validation.
- Backend property env opt-in postgres, default in-memory; fail startup on DB failure.
- Two persistence-specific exception categories.
- Runtime Flyway / JDBC dependencies (Testcontainers tests only).
- App services and existing Order HTTP endpoints unchanged.
- Nine real PostgreSQL adapter integration tests.
- Real JVM A SIGKILL JVM B same DB HTTP recovered CANCELLED through new script and GitHub Actions postgres service.
- Original P18 memory negative unchanged and still executed.
- ADR-0031, scenario, evidence, experiment ledger.

Measured guarantees:
P12 process A accepted Order remains retrievable from B after app SIGKILL, status CANCELLED, same identity/customer/restaurant/line details/total. Real JDBC Testcontainers tests recover complete local history, including payment/refund-request milestones.
Not guarantees:
No provider payment truth; not a PostgreSQL host crash/DR test. saveCurrent still stale last-write-wins/upsert, no command dedup. Read may tear across statements under concurrent writes at READ COMMITTED; database constraints incomplete; no pool/scaling benchmark; no full causal event validation. Timestamp microsecond precision caveat.
Next P13 Relational Constraints and Defense-in-Depth Integrity. Continue same production app and verified frozen P12 branch, not main.
