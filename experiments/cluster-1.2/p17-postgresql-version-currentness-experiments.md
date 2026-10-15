# C1.2-P17 — Optimistic Currentness Controlled PostgreSQL Experiments

Repeatable method for every experiment:
Engineering question -> Hypothesis -> Prediction -> Setup -> Controlled variable -> Execution -> Observation -> Evidence -> Interpretation -> Limitations -> Conclusion.
Environment: PostgreSQL16 Testcontainers, Flyway V1/V2/V3, real PostgresOrderRepository, real Order aggregate, real Spring-independent OrderWorkflowService for one application result experiment. No mocks of database update count. Race uses CountDownLatch to guarantee both readers see same version before saving.

## Before-state
C1.2-P16 verified actual PostgreSQL race: two sources at PLACED, acceptance and rejection both acknowledged; final state whichever replaced later. Frozen reference SHA c48869ff7f9282229ce130ed8209fca0b78658bf.
P16's coherent read snapshot and P15 atomic write are NOT a version-currentness guarantee.

## P17 integration suite (PostgresOrderOptimisticConcurrencyTest.java; 10 tests)
E1 Revision continuity: initial Order.place v0, PostgreSQL creation v0, new JVM-style repository reconstruction v0, acceptance v1, reconstruction v1, preparation v2; ordered history and status correct. Shows source/target revision semantics.
E2 Stale cancellation: cancellation built from PLACED v0; acceptance commits v1; stale cancellation save rejects StaleOrderVersionException expected0, acceptance and lines/history survive.
E3 Status-preserving conflict: two PAYMENT_RECORDED candidates from v0 (status both PLACED), first wins, stale second rejected; no lost payment history. Indicates checking status alone is unsound.
E4 Re-save same immutable successor after its first successful commit returns conflict; revision remains1. Important negative: not evidence of retry idempotency/previous success replay.
E5 A successor save for an absent identity does not upsert phantom Order.
E6 Forged target version6 expecting5 cannot skip stored revision0; SQL affected rows=0, rejection and no mutation.
E7 Deterministic two-thread accept/reject, both load v0 first, one wins and other conflicts; version1, exactly one corresponding workflow occurrence. Do not assert winner identity.
E8 Real PostgreSQL trigger injects history INSERT failure after version-checked root UPDATE; entire write transaction rolls back including version increment, status, children. P15 atomicity preserved.
E9 Concurrent actual OrderWorkflowService accept/reject requests each load v0; one Accepted, other Rejected(STALE_ORDER_VERSION); distinguishes business stale conflict from technical failure.
E10 Direct SQL attempt to set version -1 violates V3 CHECK, SQLState 23514.
Additional inherited PostgreSQL and memory tests now assert update-only guarded semantics, and schema tests expect 3 Flyway versions (including V1-only dirty migration remediation followed by V2+V3).

## Evidence
Candidate tested commit ffc0fb28ca37999632bfe9d199eb120cac03675d, GitHub Actions https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38759021793:
- PostgresOrderOptimisticConcurrencyTest 10 tests, failures0/errors0/skipped0.
- Root Maven reactor 204 tests, failures0/errors0/skipped0, BUILD SUCCESS.
- Original P18 in-memory process A cancelled -> JVM B 404 negative control, PASS.
- P12 PostgreSQL committed cancelled -> fresh JVM B HTTP200 same OrderId, PASS.
Older checkpoints retain BEFORE evidence; current P17 tests require winner and conflict.

## Limitations
10 deterministic schedules are not exhaustive across arbitrary thread counts/lock wait durations. No p99 benchmark or massive hot-Order contention simulation. Version CAS correctness assumes ALL authoritative writers increment revision; unmanaged SQL can defeat it. PostgreSQL old data V3 seeds version0. Revision is aggregate-wide and can produce conflicts for unrelated within-aggregate changes, an intentional tradeoff to preserve one aggregate consistency boundary. No HTTP If-Match/client-supplied revision in current endpoint. Conflict is NOT automatic business retry/idempotent replay, and no local Order version proves external Payment charge truth. P18 owns safe re-read/re-evaluate/retry, SQLState classification and reconciliation.
