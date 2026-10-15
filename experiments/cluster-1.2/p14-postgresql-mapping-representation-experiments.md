# C1.2-P14 — Real PostgreSQL Mapping and Representation Experiments

**Method for each:** Question -> Hypothesis -> Prediction -> Setup -> Controlled variable -> Execution -> Observation -> Evidence -> Interpretation -> Limitations -> Conclusion.

**Environment:** real PostgreSQL 16 Testcontainers, Flyway V1+V2, existing P12 PostgresOrderRepository, new PostgresOrderMapper, no mock JDBC. Test class: PostgresOrderMappingContractTest (7 tests).

1. Gap at start of line list: direct SQL changes position 0 to 7; V2 allows nonnegative key. Mapper now reports InvalidPersistedOrderException, not an empty/misindexed list.
2. Gap within line list: persisted 0,1,4; mapper rejects rather than silently reindexes to 0,1,2.
3. Gap in workflow history: create PAYMENT_RECORDED then ORDER_CANCELLED, corrupt position 1 to 5 by direct SQL; mapper rejects rather than fabricating history ordering. Real Order state/history invariant check remains separate.
4. Valid multi-collection mapping: three OrderLines in order, two workflow occurrences; reconstructed ID same, new Java object, exact item sequence and action sequence; derived total numerically 16.00.
5. Numeric: Java BigDecimal('5.5000') roundtrip through PostgreSQL NUMERIC; numeric compareTo equals 0, observed scale originally 4 and recovered 4; Java BigDecimal('5.5').equals(BigDecimal('5.5000')) false. Do not infer all Money semantics.
6. Temporal: Instant 2026-10-12T10:30:20.123456789Z roundtrip PostgreSQL TIMESTAMPTZ -> 2026-10-12T10:30:20.123457Z, delta 211ns. Test asserts nonidentity and <1000ns difference, not business clock order. No silent normalization policy is added.
7. Query shape: one Order with three lines and three local workflow occurrences; direct join returns 9 rows (3x3), while three-query mapper reconstructs 3 lines + 3 occurrences. Cardinality evidence, not speed benchmark.

**Actual candidate CI:** commit 8db856eae5d0db83170aad625e66c37b44668aac passed root Maven and inherited process failure/recovery scripts at https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38695864088. Logs: 175 tests, zero failures/errors/skipped; P14 class 7 tests, zero failures; BigDecimal scale before=4 after=4; Instant 211ns difference; original P18 memory SIGKILL -> missing; P12 postgres SIGKILL -> recovered CANCELLED HTTP 200.

**Non-guarantees:** no serializable reads, consistent multiquery snapshot under concurrent writer, CAS/version, large-load query latency, bounded history/line sizing, bank-grade financial precision policy, provider charge, independent event store or cross-aggregate service boundary. P15–P18 own these mechanisms.
