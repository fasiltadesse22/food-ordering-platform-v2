# C1.2-P02 — Identity, Authority and Lifecycle Continuity Experiments

Inherited frozen baseline: `checkpoints/C1.2-P01` (`2a61909a2cf3470b3917caab2abecdafe9334e81`).
Test file: `OrderIdentityContinuityContractTest`.
This part does not alter production runtime behavior.

## E1 Normal lifecycle continuity
Question: Does replacing the Java object break business identity?
Hypothesis: The Order lifecycle methods return distinct objects preserving the same OrderId and stable assignments.
Prediction: PLACED->ACCEPTED->PREPARING->COMPLETED retains OrderId/customer/restaurant/placedAt and leaves original unchanged.
Setup/variable: One Order through three public lifecycle evolutions; observe Java object references vs ID equality.
Evidence path: identity continuity test in root Maven verify.
Interpretation: Legal domain successors preserve identity; not a persistence/commit guarantee.

## E2 Distinct identity, identical state
Question: Does identical visible content imply the same business Order?
Prediction: Two different OrderIds produce two repository entries despite otherwise identical state.
Setup/variable: Hold contents and placement instant constant; vary only OrderId.
Interpretation: Current state equivalence does not imply entity identity.

## E3 Identifier value semantics
Question: Does a different Java OrderId instance lookup the same value-based key? Can CustomerId equal RestaurantId?
Prediction: UUID parse round-trip preserves OrderId value and hashCode. Equal strings in different ID wrapper types remain unequal.
Interpretation: Typed identifier value equality is distinct from entity instance reference identity; no auth claim.

## E4 Independent repository authority
Question: Does knowing the same OrderId provide current state in another process-local repository?
Prediction: A second repository instance is empty even when first holds the Order.
Limitation: Two objects inside one JVM are not a literal process restart; real P18 covers JVM replacement.

## E5 Stale representation with correct identity
Question: Does same business ID prove that a candidate remains legal to commit?
Prediction: Old PLACED object can locally construct CANCELLED after ACCEPTED becomes current, but no P02 save occurs.
Interpretation: Domain identity and temporal authority are different. P17 proves unsafe save.

## E6 Runtime failure inherited from P18
Question: Does identity continuity alone enable JVM replacement recovery?
Prediction/observation from P18: CANCELLED / HTTP 200 before SIGKILL; same OrderId HTTP 404 in process B.
No new durable recovery claim; this is a negative control.
Run via existing `experiments/cluster-1.1/p18-process-restart-durability.sh`.

## Verification gate
Root `mvn -B -ntp verify` plus real P18 script executed by GitHub Actions on the **exact P02 commit**.
Before CI completes the result is pending. Do not substitute P01 CI or invent a pass.
Record test counts and direct run URLs from GitHub Actions once finished; create `checkpoints/C1.2-P02` only after exact-commit success.
