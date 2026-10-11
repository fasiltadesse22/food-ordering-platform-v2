# C1.2-P03 — Atomicity Contract Experiments

Inherited: C1.2-P02 (e6919e51a5deb5b105397b44205153f1375eab73). P03 test file: `OrderAtomicityContractTest`.

## E1 Candidate != commit
Question: Is an invariant-valid immutable successor authoritative as soon as constructed?
Prediction: BEFORE save, current repository remains PREPARING; AFTER save becomes COMPLETED.
Controlled variable: invocation of `saveCurrent`.
Interpretation: candidate creation and authority publication are separate.

## E2 Pre-publication failure
Prediction: a repository that throws before assignment leaves current PREPARING and the use case propagates infrastructure failure.
Preexisting P11 `OrderLocalCommitBoundaryTest` provides similar negative control; P03 makes counterpart explicit.

## E3 Post-publication exception
Prediction: test repository stores COMPLETED successor, then throws; caller observes technical failure while current authority remains COMPLETED.
This is an injected **in-process** scenario, not a demonstrated SQL commit, network response loss, or financial charge.
This exposes why caller exception != proof of no effect and motivates unknown-outcome handling later.

## E4 Invalid domain decision
Prediction: attempting ACCEPT from PREPARING returns domain rejection and does not invoke repository save.
This distinguishes a domain guard failure from an infrastructure failure.

## E5 Inherited P11 partial split
Status-first, history-first and normal complete successor; compare selected invariant violations.

## E6 Inherited P13/P14/P17/P18 regression
Concurrent/stale last-write-wins and restart-loss remain expected fragile behavior, not failures that P03 fixes.

## Execution requirements
Root GitHub Actions `mvn -B -ntp verify`, including 4 new deterministic tests and existing tests;
real P18 process-restart script on exact P03 commit. No P03 pass claims until CI completes.
