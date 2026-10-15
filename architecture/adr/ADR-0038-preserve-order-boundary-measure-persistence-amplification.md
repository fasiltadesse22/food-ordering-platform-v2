# ADR-0038 — Preserve Order Aggregate Boundary While Characterizing Persistence Write Amplification and Contention

Status: Accepted design for C1.2-P19 pending exact final SHA CI and freeze.
Inherited frozen verified C1.2-P18: 752e1def3b66029d659d6344721158b40f2cf6c5.
Scope contract: V8.0 Master Roadmap plus Cluster1.2 P1.2.19 blueprint.

## Forces
Current Order root controls local business lifecycle+commercial lines+selected ordered workflow facts that must remain mutually coherent. P15 atomic transaction, P16 coherent snapshot and P17 aggregate-wide CAS are actually verified; P18 bounded local retry policy. Full JDBC child DELETE+INSERT replacement may cost 2L+2H+1 row operations per one appended history item, and root row lock persists through those operations. Need evidence under small/medium/larger data sizes and hot writer pressure.

## Decision
DO NOT split logical Order consistency boundary, move payment/restaurant authoritative state into Order, or introduce cache/queue/microservice as P19 optimization. Preserve production writer and version correctness unchanged. Add real PostgreSQL experiment suite with workload matrix, exact trigger-audited child write counts, same-Order vs different-Order barrier races, and stale large-snapshot preservation. Record wall-clock samples only as non-SLA CI observations. Establish a rigorous baseline that later allows evidence-backed alternative mapping or boundary decisions.

## Alternatives
1 Full snapshot replacement (existing): easiest consistent mapping; writes unchanged rows, potential WAL/lock amplification. Retain until quantified.
2 Append-only workflow history within SAME aggregate transaction: could reduce update cost while maintaining one authoritative Order version; requires prefix integrity, positions, SQL history mapping, rollback and concurrency tests. Evaluate after P19, not silently introduce.
3 Differential line updates within one transaction: more complex diff, ordering and duplicate-item business semantics; may be justified by measured line-count pressure.
4 Independently authoritative history aggregate: dangerous for status/history invariants; would require different coordination/consistency guarantee; not authorized by mere large H.
5 One version per OrderLine: could permit conflicting updates to bypass Order-level invariant; not currently justified.
6 Unnecessary distributed queue/Redis/cache: doesn't eliminate local correctness or row rewrite; premature.
7 One-statement aggregate query: may reduce round trips, but join product L*H and mapping costs; evaluate on measured read workload.

## Consequences and boundaries
Five deterministic integration experiments on actual PostgreSQL provide structural and concurrency evidence. JDBC wall samples are printed but not asserted as relative SLAs. Existing P15/P16/P17/P18 correctness remains tested. No production SQL changes and no new migration. P20 needs to prove any prospective undersized boundary can preserve invariants before splitting. P19 no measured production throughput, hardware profiles, WAL pressure or p99 claim.
