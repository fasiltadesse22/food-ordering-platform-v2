# ADR-0031 — Select Real PostgreSQL Authority and Validate Reconstitution Across JVM Replacement

Status: Accepted at verified P12 implementation run; final SHA verification required before freeze.
Base: C1.2-P11 1defeffc57170d86b5c0afc122f48b8f5ef826a2.

## Problem
P18 demonstrated accepted CANCELLED Order lost after replacing in-memory JVM. P11 built a real PostgreSQL schema but never wired runtime. One Order spans root, lines, workflow occurrence tables; merely writing root row cannot reconstruct a valid Order or support domain invariant checks.

## Decision
Implement minimal JDBC PostgresOrderRepository conforming to existing OrderRepository; wire through explicit backend property (default memory for inherited negatives, postgres opt-in). Run Flyway at Postgres adapter startup; fail startup if database unavailable.
Use local JDBC transaction for each aggregate write spanning root and child tables. Create via INSERT protected by existing UUID PK, detect 23505; existing saveCurrent deliberately UPSERTs and replaces all child state atomically, without version checking.
Add Order.reconstitute: controlled reconstruction of existing Order from root+ordered children, reusing selected domain invariant checks. Distinguish no root, malformed persisted state, and SQL technical failures.
Prove durability with real process A POST+cancel -> SIGKILL -> process B GET with same PostgreSQL service/OrderId (200/CANCELLED), while rerunning original memory negative (404).

## Alternative analysis
- Retain map + claim persistence due Flyway: false; reject.
- Serialize one Order JSONB field to avoid joins: would change P11 relational schema/experiment progression; defer P14.
- Use JPA / Spring Data immediately: additional mapping/flush/cascade semantics complicate first recovery experiment; JDBC chosen for explicit operations and evidence.
- Introduce Service-per-aggregate, Kafka, outbox: unrelated to local durable recovery; reject.
- Add optimistic version and @Transactional all at once: premature for P16/P17 controlled conflicts; defer.
- No transaction for root+child rows: could commit partial aggregate, violating previously established logical atomicity; reject. Minimal JDBC write transaction is required, but comprehensive transaction tests belong P15.

## Consequences / trade-offs
+ JVM replacement against same running PostgreSQL authority reconstructs the committed Order.
+ Domain reconstitution checks present stored values; corruption is explicit rather than 'not found'.
+ Existing application services untouched.
- Each operation uses fresh DriverManager connection; no pool/throughput claim.
- No read transaction-level consistent snapshot across root/child queries (default READ COMMITTED).
- saveCurrent still last-writer-wins and still allows absent insert.
- Timestamp microsecond precision differs from Java ns; specify/normalize in P14.
- Only selected domain invariants verified; causal ordering/integer positions completeness not exhaustive.
- PostgreSQL availability, WAL/durability config, disaster recovery, backup, replication, external provider effects not covered by JVM-only recovery proof.

## Revisit
P13 constraints; P14 mapping and temporal precision; P15 transaction fail/rollback; P16 isolation; P17 versions; P18 retry/conflict; later connection pool/capacity/observability when measured.
