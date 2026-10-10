# C1.2-P01 — Controlled Boundary Discovery and Reuse of Frozen Failures

Evolution type: A. No new runtime harness, SQL engine, or production change.
Base: `checkpoints/C1.1-FINAL` (d76cfdf585e19afeef21b9d522351541c5cbc07f).

## Experiment A — Status/history split
**Question:** Can completion status and required completion occurrence become authoritative independently?
**Hypothesis:** A failure between two independent updates exposes a representation violating current Order invariants.
**Prediction:** status-first leaves COMPLETED without ORDER_COMPLETED; history-first leaves PREPARING with ORDER_COMPLETED; both rejected by selected invariant checks.
**Setup / controlled variable:** Previously executed `OrderAtomicityAndConsistencyTest` test-only `FragileSplitCompletion`, vary update ordering and injected failure location.
**Inherited observation:** Both invalid partial representations were rejected by invariant validation in P11 tests.
**Evidence:** `architecture/scenarios/C1.1-P11-atomicity-boundaries.md`; test source; frozen CI.
**Interpretation:** Status and required workflow occurrence must participate in one logically coherent decision; one normal immutable successor currently supplies local publication coherence.
**Limitation:** Test-only split representations are not PostgreSQL transactions; no crash durability or isolation proof.
**Conclusion:** Logical immediate consistency dependency supported; physical transaction decision deferred.

## Experiment B — Shared-snapshot conflict
**Question:** Does validity of two independently evolved immutable Orders guarantee a correct concurrent history?
**Hypothesis:** No: replacing the current reference without expected-version checks can acknowledge incompatible decisions.
**Prediction:** both requests return Accepted; last save determines status and recorded history.
**Setup:** Frozen `OrderConcurrentConflictWindowTest`; synchronized reads with controlled save order.
**Inherited observation:** P13 verified exactly the predicted invalid history of acknowledged decisions.
**Limitations:** Test forces selected schedules, not all possible interleavings.
**Conclusion:** Boundary must include authoritative commit/conflict semantics eventually; not solved by local invariant construction.

## Experiment C — Stale successor
**Question:** Is concurrency required to violate temporal correctness?
**Hypothesis:** No: a retained older Order can overwrite newer facts later.
**Prediction:** stale save can erase cancellation/payment/acceptance history.
**Setup:** Frozen `OrderStaleStateTemporalCorrectnessTest`.
**Inherited observation:** P17 reproduced erased facts and resurrection.
**Limitation:** No version control added; no claim about future PostgreSQL isolation.
**Conclusion:** Current authority lacks freshness checks; preserve until P16/P17.

## Experiment D — JVM replacement
**Question:** Is an acknowledged and currently authoritative Order durable?
**Hypothesis:** No: process-owned map has no recovery source.
**Prediction:** after process A is killed and B starts, the same OrderId is absent.
**Setup:** Frozen `experiments/cluster-1.1/p18-process-restart-durability.sh`.
**Inherited observation:** CANCELLED / HTTP 200 before kill, same ID / HTTP 404 after restart.
**Conclusion:** Durable persistence authorized for later P12, but not introduced in P01.
**Limitations:** This is evidence for an in-memory adapter's loss, not a benchmark of PostgreSQL.

## Execution rule
The P01 candidate commit must rerun `mvn -B -ntp verify` and the P18 process experiment through the existing GitHub workflow.
Record the exact commit and action run externally before creating the frozen checkpoint.
Do not label the newly added P01 docs/tests verified based only on inherited C1.1 actions.
