# C1.2-P22 — Cluster Exit Experiments, Reproduction and Claim Limits

Method for each selected test: question; hypothesis; prediction; setup; controlled variable; execution; observation; evidence; interpretation; limitations; conclusion.

## P22 actual integrated Testcontainers PostgreSQL16 class
Path applications/food-ordering-app/src/test/java/com/acme/foodordering/adapter/out/postgresql/PostgresCluster12ExitGateTest.java. Real V1-V3 Flyway initialization through PostgresOrderRepository.

E1 Full real Order lifecycle. Q: can one business OrderId survive multiple immutable Java successors and fresh repository reconstruction after actual PG commits? H: create v0, modify v1, local payment v2, cancel v3, local refund request v4 retains typed references and selected ordered facts. P: version4 CANCELLED, exactly four ordered history entries, commercial total17.75, recovered != Java object reference. Setup fresh PG and ID; execute actual repo.createNew/saveCurrent. Caveat: PAYMENT_RECORDED/REFUND_REQUESTED are LOCAL milestones, not provider effects.
E2 Competing immutable successors. Q: can accepted v1 and canceled v1 both publish from same PLACED v0? H: first accepted commit wins, stale cancel throws StaleOrderVersionException and preserves acceptance fact. P: status ACCEPTED v1 acceptance history only. Controlled deterministic ordering of two candidates; no claim of random scheduler interleaving or HTTP behavior.
E3 Restaurant scoped actor guard through actual application service. Q: can a different RestaurantId accept a PLACED Order assigned to first Restaurant? H: domain guard rejects without DB save. P: Rejected RESTAURANT_DOES_NOT_OWN_ORDER, PG still PLACED v0 with empty history. This is ID equality check only, not authentication/Restaurant operational availability.
E4 Cross-table invalid persisted state. Q: if deliberate test-only SQL bypass sets ACCEPTED without required history, will actual repository fail safe? H: row-local checks allow update, mapper detects selected invariant. P: InvalidPersistedOrderException; test proves domain reconstruction, not that production safe repository writes wrong state.
E5 Cross-authority proposal is not admission. Q: can P21 policy propose item requirements without creating Order or decrementing live inventory? H: policy returns immutable plan, repo.findCurrentById remains empty. P: proposal stock counts and observed version, no persisted Order. No production stock repository exists, so this test MUST NOT be described as verifying stock status.

## Inherited actual failure evidence (continue CI, not duplicated)
P11 atomicity split negative controls; P12 actual JVM SIGKILL Postgres recovery and P18 in-memory JVM restart loss; P13/P14/P17 race/stale overwrites; P15 trigger-driven before-commit and deferred-commit failures; P16 actual isolation snapshot experiments; P17 PostgreSQL conditional version write; P18 actual PostgreSQL 55P03/40001 and bounded application retry; P19 trigger audit L12,H24 ->73 child-row operations and same vs different Order contention; P20 6 actual Postgres tests split status/history, failure, guarded hypothetical inventory; P21 6 domain +5 actual PostgreSQL coordination tests.
The P22 integrated tests are not substitutes for these finer-grained specific mechanisms.

## Controlled experimental observations vs guarantees
Observed data must be read from exact final CI logs; source predictions in this file must not be treated as PASS before GitHub workflow finishes. GitHub workflows execute root Maven verify and process restart negative/positive scripts. Timing and bench SLO not established by the cluster.
No actual Payment provider calls, production stock reservation SQL, operational Restaurant model, network partition, distributed 2PC/Saga, provider idempotency or external authorization have been executed.

## Reproduction
At C1.2-FINAL SHA: mvn -B -ntp verify
experiments/cluster-1.1/p18-process-restart-durability.sh
P12_JDBC_URL=jdbc:postgresql://127.0.0.1:15432/ordering_p12 P12_DB_USERNAME=ordering P12_DB_PASSWORD=<CI_TEST_PASSWORD> bash experiments/cluster-1.2/p12-postgresql-process-restart.sh
Root Maven uses Testcontainers for actual PostgreSQL integration; CI service postgres:16-alpine supports separate JVM recovery script. Do not present locally runnable P12 command without an actual matching local DB.

## Cluster exit matrix
Correct state, identity continuity: P22 E1, P02/P05 domain identity tests.
Domain guard: P22 E3, P07/P08 existing scenarios.
Relational integrity/invalid reconstruction: P22 E4, P13/P14 existing.
Atomicity: P15 real trigger rollback, P20 split controls.
Isolation: P16 tests.
Currentness, concurrency, conflict retry: P22 E2 + P17/P18.
Sizing/pressure: P19 row-operation audit; no load SLO.
Cross-authority semantics: P22 E5 and P20/P21 test-only stock.
Durability: P12 actual process termination and in-memory negative control.
Communication/mastery: accompanying assessment document and ADR-0041.
Exit requires exact SHA, zero failed tests, work + independent frozen verification; static test text is not final runtime evidence.
