# C1.1-P13 Experiment — Conflicting Operations and Concurrency Window

## Question

Can two operations that are individually legal from PLACED both return Accepted when they concurrently validate against the same Order snapshot, even though sequential execution would reject the second operation?

## Execution

GitHub Actions run:
38026585059

Command:

mvn -B -ntp verify

## Observed test result

OrderConcurrentConflictWindowTest:
- tests run: 3
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 71
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Sequential control — observed

ACCEPT executed first against PLACED and returned Accepted.

REJECT then performed a fresh authoritative read, observed ACCEPTED, and returned:
ILLEGAL_TRANSITION.

Final authority:
ACCEPTED

Final workflow:
RESTAURANT_ACCEPTED

Interpretation:
the existing state machine correctly prevents the second incompatible transition when it observes current state.

## Concurrent schedule A — observed

The coordinated repository forced both operations to capture:
PLACED

before either save.

Local successors:
A = ACCEPTED + RESTAURANT_ACCEPTED
B = REJECTED + RESTAURANT_REJECTED

Forced save order:
ACCEPTED
then
REJECTED

Both application calls returned:
Accepted

Final authority:
REJECTED

Final current workflow:
RESTAURANT_REJECTED

RESTAURANT_ACCEPTED was absent from final authority.

## Concurrent schedule B — observed

Only save ordering was reversed.

Both operations again captured:
PLACED

Forced save order:
REJECTED
then
ACCEPTED

Both application calls returned:
Accepted

Final authority:
ACCEPTED

Final current workflow:
RESTAURANT_ACCEPTED

RESTAURANT_REJECTED was absent from final authority.

## Evidence interpretation

Executed and verified:
- sequential fresh-state execution rejects the incompatible second transition;
- both concurrent operations can make locally valid decisions from the same captured PLACED state;
- both calls can report Accepted;
- the last complete successor written becomes current authority;
- reversing save order reverses surviving current state/history.

Evidence-backed inference:
the defect is in the unprotected authoritative read→decision→replacement interval, not in the lifecycle transition definitions themselves.

Not measured:
- real-world probability;
- throughput impact;
- contention frequency;
- multi-JVM behavior;
- database isolation behavior.

## Why this is deterministic

The test does not depend on scheduler luck.

A CountDownLatch ensures both operations capture the same Order before either proceeds.

A second latch forces the selected first save to complete before the other save.

Therefore the tested interleavings are controlled inputs, not accidental timing.

## Preserved fragility

No production concurrency protection was added.

The real in-memory repository remains last-write-wins across the application-level find→evolve→save sequence.

## Next

P14 applies this exact conflict mechanism to the required Customer CANCEL versus Restaurant ACCEPT race.
