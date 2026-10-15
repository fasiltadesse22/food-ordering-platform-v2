# ADR-0032 — Enforce Selected Order Row-Local Invariants with a Forward-Only PostgreSQL Migration

Status: Accepted for C1.2-P13 subject to final exact-SHA CI gate.
Inherited baseline: checkpoints/C1.2-P12 f28facdf54366f689a2a5d278864a8d8dcc754bb.

## Context
P11 proved real PostgreSQL V1 permitted direct SQL quantity=0 and negative price although Java OrderLine rejected them. P12 could detect such corrupt values during aggregate reconstitution but did not prevent storage. Durable invalid data is an integrity risk across scripts, imports, DBA actions and faulty adapters. The current Order status and workflow action/participant vocabulary and list positions are additionally representable by selected row-local checks.

## Decision
Preserve V1 immutably; add V2__add_order_local_integrity_constraints.sql with 11 named CHECK constraints for:
- nonblank core Order identity references (ASCII-space semantics);
- known Order statuses;
- nonnegative child positions;
- nonblank menu item identifier/name;
- quantity >0;
- nonnegative finite unit_price while allowing zero and arbitrary decimal scale;
- known workflow action and participant labels.
Keep existing NOT NULL, PK, FK. Do not add Payment/Restaurant/Customer aggregate tables or an Order version.
Amend P11 and P12 expectations to assert SQLState 23514 for formerly permitted invalid line data; keep other P12 corruption tests and P17 stale overwrite negative. Add PostgreSQL Testcontainers constraints and dirty V1->V2 migration tests.

## Alternatives and trade-offs
A. Java-only validation (prechange): simple but bypassable by direct SQL; rejected as sole protection.
B. Full business domain model recreated via triggers/stored procedures: complex, duplicates domain decisions, potentially difficult to evolve, cross-row transactional/causal pitfalls; not chosen.
C. Selected row-local CHECK constraints (chosen): portable intent, database-independent writer protection, straightforward 23514 classification; incurs migration and Java/SQL rule synchronization.
D. PostgreSQL enum data type: possible tighter status typing, but type evolution/cross-app compatibility trade-offs; TEXT+CHECK appropriate for current stage.
E. Automatic cleanup of existing invalid rows inside V2: could erase or silently modify historical business data without approved policy; rejected.
F. Staged NOT VALID / VALIDATE for large tables: valuable when lock/scan costs justified, deferred to measured operational context.

## Important precision
Postgres CHECK treats UNKNOWN as satisfied; NOT NULL is essential. btrim != full Java String.isBlank Unicode. NUMERIC includes NaN/infinities not represented by Java BigDecimal, hence explicit nonfinite exclusion. Allowed enum strings do not enforce legal transitions. Rule 'each Order has >=1 line' is cross-row, not protected by FK. Required workflow milestones and actor/provenance truth are NOT protected by V2. No automatic payment validation or optimistic CAS.

## Outcomes and remaining risks
Direct invalid DML fails at PostgreSQL boundary (23514) before invalid data persists in tested case. V2 refuses deployment on existing dirty V1 data until explicit remediation; no automatic data loss/correction. Even after V2, status COMPLETED with no required history is relationally accepted and rejected by P12 reconstitution, and parent Order without lines still possible. Multi-query reads can tear under concurrent changes (P16); stale save still overwrites (P17); payment authority remains independent.
