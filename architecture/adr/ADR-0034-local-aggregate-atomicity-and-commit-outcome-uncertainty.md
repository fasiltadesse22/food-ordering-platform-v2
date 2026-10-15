# ADR-0034 — Preserve Local Order Write Atomicity and Explicitly Classify COMMIT Uncertainty

Status: Accepted for C1.2-P15, pending exact final SHA and frozen-branch verification.
Inherited frozen P14 at 9143f1f627423ad8da4cef1a41c8ae20d0045775.

## Context
Order is one immutable domain aggregate represented by orders, order_lines and order_workflow_occurrences. Its legal transitions couple status, lines and selected workflow occurrences. P12 implemented local JDBC transactions but had not injected actual failures at each write/commit boundary; all SQLException paths used a generic persistence exception. PostgreSQL may have committed a transaction when the client loses its acknowledgement. P15 must prove rollback and distinguish unknown COMMIT outcome without prematurely addressing P16 isolation/P17 optimistic currentness/P18 retry policy.

## Decision
Keep one PostgreSQL JDBC transaction per complete Order create/save:
setAutoCommit(false) -> root writes -> owned line/history writes -> commit. Roll back on pre-commit errors.
Track commitAttempted. A caught SQLException at or after COMMIT invocation becomes OrderCommitOutcomeUnknownException, a typed subtype of OrderPersistenceException that carries OrderId and explicit unconfirmed outcome semantics. It does not claim server rollback. Catch before commit preserves create-only duplicate identity exception and generic technical persistence failure.
Introduce package-private ConnectionSource injection only to permit a test JDBC proxy that successfully calls actual PostgreSQL commit and then simulates the lost client acknowledgement. Public constructor and all existing application wiring unchanged.
Use real PostgreSQL DML triggers, one deferred constraint trigger, and real backend termination to prove rollback at controlled failure windows. Preserve fragile autocommit split-write counterexample as test-only evidence.

## Alternatives
A. Reliance on @Transactional annotation without failure experiment: insufficient evidence, and an annotation does not by itself choose transaction boundaries correctly. Reject as P15 proof.
B. Separate auto-committed writes per table: invalid status/history partial commits demonstrated; reject.
C. Generic exception meaning known rollback for every SQL error: false around COMMIT acknowledgement; reject.
D. Always retry failed commands: risks duplicate Order IDs/effects; defer P18 until stable operation identity and reconciliation are designed.
E. Two-phase commit / Saga / Kafka outbox / provider global transaction: not justified for one local PostgreSQL authority; do not introduce.
F. JDBC savepoints for every table: would allow partial rollback but not the required single aggregate commit; unnecessary.
G. Database triggers mirroring complete domain state machine: unrelated to local transaction atomicity, duplicates business rules and has enforcement-order complexities; reject.
H. Immediate SERIALIZABLE/CAS: P16/P17 scope; defer.

## Trade-offs and guarantees
Observed: under injected before-commit DML errors, root/child changes roll back together; after real PostgreSQL backend kill without commit, prior committed state remains; under deferred COMMIT trigger failure, PostgreSQL rolls back previous writes; under synthetic post-commit ack loss, database nevertheless contains new committed state. Separate connections observe precommit old state and postcommit new state. Current saveCurrent remains unguarded last-writer-wins; one atomic write is not a serializable business decision.
The unknown-outcome exception is conservative: some commit SQL errors can be known aborts to database observers; generic client classification does not blindly assume either outcome. Rollback after the DB has committed cannot reverse it; compensation is a distinct business operation.
No Payment external authority or distributed atomicity; no performance, RTO/RPO or high-availability benchmark.
