# C1.2-P07 — Root Encapsulation and Invariant Experiments

Base SHA: `edde75f6dc63cb4c1568939497ab7cea218c5759`.

## E1 — Verified gap / selected new rule
Question: Can valid individual ACCEPTED fact and PLACED status combine into a verifier-accepted but unreachable business representation?
Source-based before observation: previous checker had no accepted-fact reverse implication. Prediction after change: invariant violation code ACCEPTANCE_FACT_REQUIRES_ACCEPTED_OR_LATER_STATE.
Experiment: package-scope invocation with PLACED + RESTAURANT_ACCEPTED.

## E2 — Competing terminal states
REJECTED with both RESTAURANT_REJECTED and RESTAURANT_ACCEPTED, and CANCELLED with ORDER_CANCELLED plus RESTAURANT_ACCEPTED must fail new invariant.

## E3 — Legal progression regression
Public PLACED -> ACCEPTED -> PREPARING -> COMPLETED must remain legal; source identity preserved and earlier representations unchanged.

## E4 — State exposure and aliasing
Modify caller's original ArrayList after Order.place and try mutating getter-owned lists. Current state must remain unaffected.

## E5 — Operation guards
COMPLETE from PLACED rejected as illegal transition; acceptance by a different RestaurantId rejected without altering state.

## E6 — Refund guard
Recorded payment -> cancellation -> refund request accepted once; repeated request rejected; earlier immutable representation unchanged.

## E7 — Intentional negative control
Two recordPayment calls yield two local PAYMENT_RECORDED occurrences. This documents the **current absence** of provider idempotency/duplicate fact guard; it is NOT a successful financial reliability guarantee.

## Required verification
`mvn -B -ntp verify` and the real P18 SIGKILL restart script on exact P07 evidence-bearing commit through GitHub Actions. P13/P14/P17 fragile scenarios remain expected. Do not assert successful results until CI is observed.
