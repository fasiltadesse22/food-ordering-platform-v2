# C1.2-P15 — Real PostgreSQL Atomicity, ROLLBACK and COMMIT Uncertainty Experiments

Question -> Hypothesis -> Prediction -> Controlled setup/variables -> Execution -> Observation -> Evidence -> Interpretation -> Limitations -> Conclusion.
Environment PostgreSQL 16 Testcontainers + Flyway V1/V2; real PostgresOrderRepository and immutable Order; test-only functions/triggers created inside container and removed before subsequent tests. All actual rollback/commit SQL remains PostgreSQL, not a Java fake. Only acknowledgement failure is simulated *after real db.commit()* through a JDBC Connection proxy; no real network partition is claimed.

Test source: applications/food-ordering-app/src/test/java/com/acme/foodordering/adapter/out/postgresql/PostgresOrderTransactionBoundaryTest.java. Ten tests.

E1 Create new Order, real BEFORE INSERT trigger rejects line insertion after root INSERT; repository returns OrderPersistenceException (not commit-unknown); independent connection shows zero root, lines, history. Prediction: no partial new Order committed. PASS.
E2 Existing Order PLACED, accept successor; trigger rejects line INSERT after UPSERT root and delete old children; original PLACED status, original two lines, empty history remain. PASS.
E3 Existing Order PLACED, accept successor; trigger rejects workflow occurrence INSERT after new lines written; original full state survives rollback. PASS.
E4 DEFERRABLE INITIALLY DEFERRED constraint trigger on history INSERT only raises at COMMIT; repository reports OrderCommitOutcomeUnknownException conservatively, independent DB verification proves original state (PostgreSQL rolled back this known injected failure). PASS. Explicit difference between server knowledge of this test and generic caller error semantics.
E5 JDBC writer opens transaction, updates root status, inserts acceptance occurrence, independent repo read before commit returns old PLACED + empty history; after COMMIT shows ACCEPTED and acceptance occurrence. PASS. Does not prove snapshot fidelity across concurrent multiple SELECTs (P16).
E6 Writer starts real PostgreSQL transaction, updates root+history without COMMIT; independent connection runs pg_terminate_backend(writer backend PID); server aborts transaction, original PLACED/lines/history survive. PASS. This is actual backend termination, not simulated JVM shutdown; P12 independent JVM SIGKILL after committed data remains positive.
E7 Test-only raw separate autocommit UPDATE sets ACCEPTED without history; committed invalid representation causes InvalidPersistedOrderException on read, demonstrating atomic boundary necessity. PASS. This is a deliberate negative control, never normal repository behavior.
E8 Test package-private ConnectionSource returns JDBC proxy invoking actual db.commit() then throwing SQLState 08006 to imitate acknowledgement loss. createNew raises OrderCommitOutcomeUnknownException, independent repository loads committed Order, repeated createNew same ID hits 23505 conflict. PASS. No proof of real network failure behavior or business command idempotency.
E9 Same post-commit acknowledgement proxy on saveCurrent(accepted successor). Caller sees OrderCommitOutcomeUnknownException, independent read sees ACCEPTED with history. PASS.
E10 Ordinary createNew and saveCurrent acceptance succeed; a second independent PostgresOrderRepository retrieves whole committed Order and row counts root=1, lines=2, history=1. PASS.

Implementation candidate exact SHA 4ac8353f0a36dcfe7039d0a24da6da499c4e14e7 GitHub Actions run https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38697855420 SUCCESS. Maven 185 tests, 0 failures/errors/skipped; P15 class 10 PASS; original P18 memory negative and P12 JDBC JVM-positive both PASS. Exact final docs SHA requires another CI pass and frozen SHA verification.

Unknowns: client exception after COMMIT may mean database committed or aborted; before-commit unexpected connection/rollback failure requires cautious diagnosis. Do not claim automatic recovery or exactly-once external effects. The transaction boundary remains PostgreSQL-local. Current isolation read issue (P16) and stale overwrite (P17) preserved.
