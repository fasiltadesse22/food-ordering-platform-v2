# C1.2-P21 — Controlled Domain and Real PostgreSQL Coordination Experiments

Protocol: engineering question -> hypothesis -> prediction -> setup -> controlled variable -> execution -> observation -> evidence -> interpretation -> limitations -> conclusion.
All tests use code in food-ordering-app. Six domain-policy unit tests plus five PostgreSQL16 Testcontainers integration tests. Experiment schema p21_stock and p21_reservations is test-only, not production. Context C1.2-P20 parent commit 36284935e29f00a5f4949cda2956bf66e0d72356.

U1 Duplicate menu line aggregation: Order lines burger1+burger2, water1 -> plan burger3, water1, unchanged Order version/history. Demonstrates no per-line oversell bypass.
U2 Insufficient aggregate quantity: burger1 and burger2 vs observed2 => typed INSUFFICIENT_OBSERVED_QUANTITY.
U3 Restaurant mismatch: Order.restaurantId does not match snapshot.restaurantId => WRONG_RESTAURANT; no authority crossing.
U4 Unknown item: absence from observation => ITEM_NOT_IN_SNAPSHOT (not invented zero availability).
U5 Non-PLACED Order: proposed snapshot check rejects under narrowly specified planning workflow.
U6 Immutable snapshot/plan: copying from caller's mutable Map, plan Map can't mutate; observedVersion is carried but NOT commit-time proof.

PG1 Atomic same-local-database: test-only helper begins JDBC tx; inserts Order root and OrderLines, conditionally decrements p21_stock, inserts reservation with FK to Order, COMMIT. Real production OrderRepository independently reconstitutes placed Order; stock remaining0, reservation count1. Demonstrates separate logical authorities can share local transaction.
PG2 Order-first partial: actual OrderRepository.createNew commits Order. Stock changes after earlier eligible proposal; test-only attempt to allocate fails conditional UPDATE. Order remains PLACED, no reservation. This is not an inherent domain invalidity absent explicit strong-at-placement requirement, but is a partial workflow that needs policy.
PG3 Stock-first partial: conditional stock decrement commits independently; Order never created, reservation absent. Capacity reduced without Order association; recovery/expiry needed.
PG4 Later SQL failure inside shared tx: test-only PostgreSQL trigger throws P0001 during reservation INSERT after root+line insert and stock decrement. Entire transaction rolls back: Order absent, remaining1, no reservation. This is real DB fault injection, not JVM crash/remote partition.
PG5 Two racing plans from same snapshot: both Order proposals pass pure service with observed remaining1. Coordinating two actual PostgreSQL transactions, only one stock conditional UPDATE succeeds, commits Order+reservation; loser rolls back its new Order. Final remaining0, reservation1, exactly one Order persisted. Shows planning!=admission.

Nonclaims: Test-only JDBC coordination is NOT wired into PlaceOrderService or production HTTP. No real Inventory authoritative repository, no Restaurant open/closed model, no external Payment provider, no cross-service atomic commit, no actual P21 crash/partition, no benchmark. Legacy PlaceOrderService still creates Order independently; thus the application has NO universal stock-before-placement guarantee. P21 establishes mechanism and domain policy groundwork, not complete product behavior.
Verification source: exact SHA GitHub CI run after documentation and independent frozen branch check, root Maven reactor and inherited P18 in-memory negative + P12 PostgreSQL process restart positives. No PASS claim until logs checked.
