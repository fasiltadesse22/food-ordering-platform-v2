# ADR-0039 — Do Not Split Current Order Status and Required History Into Independent Commit Authorities

Status: Accepted P20 design pending final same-SHA CI checkpoint freeze.
Inherited verified C1.2-P19 commit 3c1d8b56fd2cb1b1f30b2fb95ae2e0a5c2b7e8ae.
Authoritative Master Roadmap V8.0 Cluster1.2 P20.

## Context
P19 proves the current repository rewrites owned lines and history, creating work amplification, and some conceptually different operations contend on one Order version. An attractive but dangerous optimization is splitting status and history into separately committed aggregates. Inspected OrderInvariants.verify requires acceptance history when status ACCEPTED; acceptance fact cannot appear while status PLACED. PostgreSQL V2 checks do not enforce this cross-row rule. P15 currently makes status/history one atomic transaction; P16 one read snapshot; P17 one root version; P18 bounded conflict resolution.

## Decision
Preserve Order status, required workflow occurrences, commercial line state, and root revision as one logical Order consistency boundary. Do not create independently committing OrderStatus and OrderHistory aggregates to address P19 child-row rewriting. Introduce only negative-control test-only SQL demonstrating split/status-first, split/history-first, second-phase failure; positive single-transaction control and separate test-only inventory oversell/guarded-admission pair. No production tables/service decomposition, payment integration, inventory model or feature added.

## Why
Committed status-first ACCEPTED with no acceptance fact and history-first acceptance with PLACED both satisfy selected row-level SQL constraints yet fail domain reconstitution. An independently failed second transaction cannot roll back first commit. Snapshot isolation cannot heal an invalid state that really committed. A shared transaction (even across several tables) protects the required local invariant; physical append-oriented mapping may reduce write work without weakening ownership. Separate aggregate identity and separate transaction boundaries are NOT identical, but independent authority requires explicit coordination for hard invariants.

## Alternatives
1 Keep existing verified full-child-rewrite transaction: simple/correct, P19 measured row churn.
2 Append-only history within same root conditional version transaction: plausible better physical cost; requires validation of prefix consistency, ordering, rollback, duplicated updates.
3 Split conceptual model but commit both in one transaction: possible local mechanism, trades aggregate clarity for cross-object coupling; only justify with domain evidence.
4 Independent status/history transactions: rejected under current invariant without alternative enforcement.
5 Async eventual history repair: can be correct only with explicitly changed business temporal contract and reliable recovery; not justified.
6 Distributed mutex: cannot atomically commit two remote stores or prevent crash between successful commits; rejected.
7 Merge entire Restaurant/Payment truth under Order: rejected as false authority.
8 Inventory shared CAS within one local transaction: demonstrated test-only proof of a narrow stock admission invariant; no actual production Inventory service or remote coordination.

## Consequences
No production code change; new executable PostgreSQL failure evidence supports preservation of current aggregate. API/read mapping correctly rejects committed cross-table partial state, rather than silently reconstructing corrupt Order. Test-only tables are not a shipped feature. Next P21 evaluates genuinely cross-aggregate operations under explicit business consistency requirements, future Payment provider truth and Restaurant/inventory authority.
