# ADR-0022 — Separate Logical Order Atomicity From Physical Durable Commit

Status: Accepted for P03 requirements and mechanism deferral, NOT a claim that DB atomicity is implemented.
Inherited verified checkpoint: `checkpoints/C1.2-P02`, SHA `e6919e51a5deb5b105397b44205153f1375eab73`.

## Context
P01 derived immediate Order consistency dependencies; P02 established stable identity but process-local transient authority.
P11 verified normal OrderEvolution + immutable successor and two deliberately invalid partial split representations.
P13/P14 found incompatible acknowledged decisions despite complete successors; P17 found stale overwrites; P18 found process restart loss.

## Decision
Document a future local atomic persistence contract for related changes, especially status+required history.
Preserve current single immutable successor and process-local publication path as a baseline.
Add a controlled adapter-failure test separating:
(1) candidate existence;
(2) publication;
(3) caller result.
Do not implement SQL transactions, schema, JPA, Kafka, locks, idempotency or services in P03.

## Reasons
Atomicity belongs to the authoritative commit of one logical business decision, not simply to construction of a Java object.
One map-entry assignment publishes one complete current in-memory representation but not a durable transaction.
Failures after in-memory publication can differ from failures before it; an exception does not universally prove an operation failed.
A future DB transaction is justified for physically separate writes that together constitute the required persisted Order decision.
No evidence supports widening the atomic boundary to external payment-provider state.

## Alternatives
Keep status/history independently committed (violates P11 invariant); use derived eventually consistent history (different business contract);
introduce immediate PostgreSQL transaction P03 (premature before relational design); use generic retry (does not eliminate partial-state or unknown outcome).

## Consequences and limits
P03 changes tests/docs only, does not fix lost updates, stale state, repeated commands, process restart or provider split-brain.
A physical local transaction may commit atomically and still allow stale business decisions depending on isolation/control.
Future: P11/12 real storage, P15 local transaction and rollback proof, P16/17 isolation/freshness, P18 unknown outcome/retry semantics.

## Revisit triggers
Business invariants change, workflow history becomes projection rather than authoritative, multi-aggregate local transaction justified,
measured contention requires narrower commit boundaries or external authority coordination is introduced.
