# C1.1-P13 Experiment — Conflicting Operations and Concurrency Window

## Question

Can two operations that are individually legal from PLACED both return Accepted when they concurrently validate against the same Order snapshot, even though sequential execution would reject the second operation?

## Hypothesis

The current find→evolve→save workflow has no version/compare-and-set protection.

If ACCEPT and REJECT both capture the same PLACED Order before either saves:
- both domain decisions will be locally valid;
- both application calls can return Accepted;
- last save wins;
- reversing save order reverses final authority.

## Prediction

Sequential control:
- ACCEPT succeeds;
- subsequent REJECT reads ACCEPTED and is rejected as ILLEGAL_TRANSITION.

Concurrent schedule A:
- both read PLACED;
- ACCEPT saves first;
- REJECT saves second;
- both results are Accepted;
- final authority is REJECTED;
- acceptance occurrence is absent from final authority.

Concurrent schedule B:
- both read PLACED;
- REJECT saves first;
- ACCEPT saves second;
- both results are Accepted;
- final authority is ACCEPTED;
- rejection occurrence is absent from final authority.

## Setup

Use the real OrderWorkflowService.

Use a test-only CoordinatedConflictRepository implementing the real OrderRepository port.

The test repository:
- captures the current Order before its read barrier;
- waits until two reads captured that same snapshot;
- deterministically orders saves using a latch.

Production repository behavior remains unchanged.

## Controlled variable

Only successor save ordering changes between the two concurrent experiments.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P13 CI.

## Evidence interpretation

A passing deterministic conflict test will establish that the current business workflow permits the selected stale-snapshot interleavings under the test harness.

It will not establish:
- probability/frequency under real traffic;
- behavior across multiple JVMs;
- database isolation behavior;
- the correct locking/versioning mechanism.

## Limitations

P13 deliberately does not:
- execute customer cancellation against restaurant acceptance;
- add concurrency protection;
- measure contention;
- introduce PostgreSQL.

P14 owns the exact cancellation/acceptance business race.

## Conclusion

Pending execution evidence.
