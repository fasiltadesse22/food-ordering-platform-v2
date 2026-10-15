# ADR-0033 — Validate Relational List Positions During Order Reconstitution; Retain Explicit Three-Query JDBC Mapping

Status: Accepted for C1.2-P14, subject to final SHA CI gate.
Inherited verified checkpoint C1.2-P13 9fd1fe7c3c9a0608c29af5c8397e8bfc3ae9588e.

## Context
PostgreSQL stores Order and two ordered child collections. The P12 reader sorts by persisted position but previously ignored actual position values. P13 enforces nonnegative positions and per-Order uniqueness, but gaps 0,2,5 remain legal. Mapping such rows into Java List silently collapses positions, concealing a malformed representation. Domain OrderLine(BigDecimal) and workflow Instant have value/precision trade-offs.

## Decision
Extract a package-private PostgresOrderMapper with explicit SQL-row conversion and controlled Order.reconstitute. It rejects noncontiguous line_position and occurrence_position (requires 0..n-1), reports malformed persisted state as InvalidPersistedOrderException, and preserves selected domain invariant checks. Keep existing separate SELECT queries, three relational tables, unchanged schema and JDBC backend selection. Do not change read isolation or successor update semantics in P14.

## Alternatives considered
- Ignore positions and merely ORDER BY: silent lossy re-indexing; rejected.
- Add DB triggers/procedures for contiguity: transaction-side ordering complexity and lock issues; unnecessary for P14; reject.
- JSONB root document: fewer relational child records, different constraint/index/representation trade-offs; not justified.
- One JOIN fetching two collections: single SQL statement but LxH multiplicative rows and regrouping; P14 SQL experiment demonstrates cardinality, no p99 comparison.
- JPA/Hibernate rewrite: additional identity-map, flush, proxy, cascading, N+1/multi-bag semantics without a failure requiring ORM; defer.
- Timestamp truncation or Money scale normalization in domain: imposes business policy not yet established; reject.
- Change isolation to REPEATABLE READ: P16 owns proof for concurrent query snapshot anomalies; defer.
- ExpectedVersion CAS: P17 owns stale-write solution; defer.

## Consequences
+ Detects malformed persisted collection indexes before returning domain object.
+ Keeps data representation and domain reconstruction code separated without new public domain concepts.
+ Measured PostgreSQL conversion: BigDecimal 5.5000 -> numeric equal, scale 4 in tested case; Instant .123456789 -> .123457 (211ns difference), so exact Instant nanos not guaranteed.
+ Existing P12 recovery and P13 constraints remain.
- Three round trips to DB for an existing Order; no load benchmark.
- Consecutive reads across different commit snapshots still possible; history and status may be mismatched under concurrent write. P16 will analyze.
- Current full child replacement and P17 stale last-writer-wins remain.
- Domain OrderInvariants selected implications are not complete causal event proof.
