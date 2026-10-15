# ADR-0035 — Reconstitute Order with One Read-Only REPEATABLE READ Snapshot

Status: Accepted for C1.2-P16 pending exact evidence SHA / frozen CI verification.
Inherited source: C1.2-P15 aae54a4346f190cc02ff809b241c6fc64c11d3b8.

## Problem and evidence
PostgresOrderRepository.findCurrentById previously issued three SELECT statements using default READ COMMITTED behavior. P15 made writes atomic, but two reader statements can observe different committed snapshots. Real PostgreSQL P16 test reproduced root PLACED and later history RESTAURANT_ACCEPTED even though writer's transaction committed the valid combined ACCEPTED+history atomically. Calling Order.reconstitute on that mix correctly rejects it, but storage need not be corrupt. This is an isolation/read-consistency failure at aggregate boundary, not a failed writer transaction.

## Decision
On each full Order lookup, start a bounded read-only JDBC transaction at TRANSACTION_REPEATABLE_READ before first SELECT, read root/ordered lines/history, reconstruct Order through existing validated mapper, and commit the read-only transaction. Rollback on exceptions. Retain previous write-side atomic transaction and explicit COMMIT uncertainty. Preserve current port and one deployable. Add package-private RootReadObserver NOOP in production for deterministic concurrent test (root-before-children interleaving). No changes to schema, domain invariants, optimistic versions, or retry protocol.
Verified actual repository test holds read after first root SELECT, writer commits acceptance, reader reconstructs coherent old PLACED+[], next reader loads ACCEPTED+acceptance. Reader does not block writer in this setup.

## Trade-offs and alternatives
1 READ COMMITTED sequential SELECT: proven nonrepeatable/torn aggregate, rejected for this full Order repository.
2 REPEATABLE READ read-only snapshot: chosen, simple and nonblocking for normal SELECTs, but snapshot may be stale after writer commits, and extra transaction/snapshot resource cost must be measured.
3 Single statement JOIN/aggregation: can also obtain statement snapshot but changes mapping/query shape, multiplicative child cardinality in P14, unnecessary now.
4 SELECT FOR UPDATE: blocking writer/locker, NOWAIT 55P03 under contention, does not make separate read/save transactions correct. Not selected for read-only projection/reconstruction.
5 SERIALIZABLE: stronger transaction guarantees for whole read-modify-write, but possible SQLState 40001 and retry needs, P17/P18 boundary.
6 Optimistic expectedVersion/CAS: natural for stale immutable successor detection, specifically P17.
7 Distributed lock/Redis/EDA: wrong scope for one local PostgreSQL aggregate read.

## Evidence-bound conclusion
Nine new real PostgreSQL tests passed implementation CI at https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38752561425 (candidate SHA 1e12bfb1b94245d26fb2cd347b7a852104044c57), 194 root Maven tests pass; original P18 memory JVM process-loss 404 and P12 durable PostgreSQL recovery 200 passed.
- READ COMMITTED can mix two committed aggregate epochs across statements.
- PostgreSQL REPEATABLE READ preserves one transaction snapshot.
- Actual repository reproduces consistent precommit state while concurrent writer commits.
- lock_timeout and SELECT FOR UPDATE NOWAIT yield SQLState 55P03 when conflicting.
- PostgreSQL REPEATABLE READ UPDATE after concurrent commit can return 40001.
- Two application requests that read same PLACED state and save mutually exclusive decisions still both succeed under unconditional saveCurrent. Atomic SQL write + consistent past read != version guarded freshness.
No p95/p99 performance claim, no complete serializability, idempotency, Payment provider authority, or disaster recovery promise.

## Dependency handoff
P17 add explicit version/currentness protection to actual repository under same project, preserve before-case. P18 controlled conflict/serialization-retry/idempotency semantics.
