# ADR-0036 — Use an Aggregate Revision and PostgreSQL CAS for Order Currentness

Status: Accepted in C1.2-P17 subject to final exact-SHA CI and frozen-branch verification.
Inherited verified C1.2-P16 c48869ff7f9282229ce130ed8209fca0b78658bf.

## Problem
P16's real race showed two valid immutable successors from one PLACED state could both be acknowledged, despite atomic multi-table writes and coherent MVCC reads. Second update uses obsolete business source state and overwrites first. Even checking status is insufficient because recording payment and modifying lines can keep status PLACED. Need detect the exact source aggregate revision at commit.

## Decision
Give immutable Order a nonnegative long version. New placement 0; each accepted local domain successor increments exactly once with Math.addExact. Read stored revision in same P16 snapshot and pass to Order.reconstitute. Add V3 Flyway orders.version BIGINT NOT NULL DEFAULT 0 CHECK >=0; historical V1/V2 rows receive baseline zero. Replace unconditional SQL upsert with UPDATE orders SET fields, version=successor.version WHERE id=? AND version=successor.version-1; assert one affected row. Keep P15 child replacement under the SAME physical transaction. Zero-row update => StaleOrderVersionException (OrderId + expectedRevision), and rollback; no child write. No insert for missing identity in saveCurrent. Mirror atomic compute-if-version in process-local memory adapter. Application maps stale conflict to distinct result and HTTP 409. Do not retry automatically.
Retain all earlier domain invariants, P14 mapper, P15 COMMIT uncertainty and P16 read-snapshot coherence.

## Alternatives
1. Application-side check-then-write: race between separate SQL statements; rejected.
2. Compare only status: misses status-preserving payment and line changes; rejected.
3. Pessimistic root lock through command: viable if whole decision/write transaction held, adds contention/resources and changes service transaction scope; not necessary.
4. SERIALIZABLE service transaction: viable but can return 40001 requiring full transaction replay; P18 policy deferred.
5. Encode version in Order versus repository VersionedOrder envelope: chose Order property for continuity with existing immutable transitions, simplifying service port; incurs technical revision state in domain representation. Version is not business identity or money/event authority.
6. Distributed locking/message bus/event sourcing: not warranted by one PostgreSQL aggregate CAS.
7. Automatic retries or state merge: potentially unsafe with acceptance/rejection or external payments; defer P18.

## Guarantees and explicit limitations
Real PostgreSQL tests show exactly one winner for two candidates from same version; stale write is rejected without modifying version/root/lines/history; rollback of later SQL error also reverts root version. Repeat same successor is rejected, but that is not semantic idempotency or response replay.
The SQL UPDATE predicate is enforced by the server; separate Java read/check does not offer this guarantee. A stale conflict can mean changed or absent identity, do not invent current revision in exception.
All writers must participate. V3 migration seeds revision0 but does NOT reconstruct historical counts; mixed legacy writers present a rollout hazard. HTTP does not yet accept a client expected revision/If-Match; stale UI intent may not be detected as such when server initially reads latest Order for a fresh request. No guarantee for independently authoritative Restaurant, Customer, Payment or provider effects.
P17 creates an explicit version conflict signal; P18 owns re-evaluation, safe retries, idempotency, reconciliation and circuit/locking policies.
