# C1.2-P19 — Executable Aggregate Size / Contention Experiment Protocol

Method: question -> hypothesis -> prediction -> setup -> controlled variable -> execution -> observation -> evidence -> interpretation -> limitations -> conclusion.

Repository: one continuous Food Ordering Platform inherited from frozen P18 752e1def3b66029d659d6344721158b40f2cf6c5. Actual Testcontainers PostgreSQL16, Flyway V1-V3, actual JDBC repository, Order immutable aggregate and CAS guard. All E1-E5 tests in PostgresOrderAggregateSizingTest.java.

E1 L/H workload matrix:
Q does growing Order owned state increase rows reconstructed/replaced?
H full repository read remains 3 SELECT statements but transfers 1+L+H rows, one new milestone results in 2L+2H+1 child DELETE/INSERT operations.
Profiles: (1,0)->2 reads/3 child ops; (8,16)->25 reads/49 child ops; (48,96)->145 reads/289 child ops.
Test seeds test-only synthetically aged PLACED Order via reconstitute with ORDER_MODIFIED history and revision baseline0 (consistent with legacy migrated baseline; synthetic not authentic customer history), stores through real repo, measures 3 read wall nanos and 1 save wall nanos, asserts exact final lines/H+1/version1, prints P19_SAMPLE... outputs. No timing threshold or monotonic claim; source workload has varying L/H together, cannot isolate which causes latency.
E2 exact write amplification:
Q does actual PostgreSQL see full DELETE/INSERT on unchanged children?
H for L12/H24 one payment milestone creates L=12 DELETE/INSERT lines, H=24 DELETE and H+1=25 INSERT history, total73 child row operations.
Temporary test-only permanent-in-test-database AFTER row triggers record counts; assert exact operation counts and final invariant; instrumentation not production schema and distorts latency. Output P19_CHURN_AUDIT....
E3 same Order contention:
Q do line-change and milestone from same version conflict despite distinct conceptual fields?
H yes one CAS root v0, one winner v1, one StaleOrderVersionException, entire winner preserved. Barrier ensures both load v0 before release. No predetermined winner.
E4 distinct Orders:
Q is root version globally contended?
H separate Order IDs each v0->v1, both succeed without version conflict, though global DB CPU/IO resource contention still possible.
E5 larger data correctness:
Q could stale large replacement erase winner?
H CAS stale candidate rejected, final 48 lines/97 history/v1 after winner milestone.
 
Evidence grading: P19_SAMPLE nanos are observed sample only when CI logs collected; E2 audit counts and E3/E4 outcomes are asserted structural evidence, independent of time. Full root Maven gate and P18 memory SIGKILL negative + P12 PostgreSQL recovery must pass on final doc-bearing SHA and frozen checkpoint. Do not claim CI PASS until logs inspected.

Potential followup measurement (NOT executed in P19): independently vary L vs H; stable warm up, fixed arrival process, p50/p95/p99 durations, transaction lock windows, PostgreSQL EXPLAIN (ANALYZE, BUFFERS, WAL) and WAL/IO per operation; connection pooling; JVM allocations; stress contention 1/2/4/8 writers per Order; migration to delta writes proof before/after. CI timing never a production SLO.
