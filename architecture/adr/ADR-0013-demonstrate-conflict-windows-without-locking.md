# ADR-0013 — Test-only deterministic overlapping-command harness; do not solve concurrent publication yet

**Status:** Accepted for new-lineage V8 Part 1.1.13.

## Context

The independently verified P12 Type A checkpoint documents a real weakness: `OrderWorkflowService.update` performs read, local guard/construction, then an unconditional current-Order save. P10's immutable aggregate and history checker cannot prevent two separately valid successors of the same stale Order from overwriting each other. P11 already contained a **deterministic sequential stale-snapshot overwrite test**, but P13 requires actual two-thread scheduling and precise read/write traces.

## Decision

1. Add a dedicated **test-only** Java JUnit harness `OrderConflictingOperationsConcurrencyTest`.
2. Run two real named JVM threads with a `CountDownLatch` barrier at repository reads. Capture the current Order value before the barrier, ensuring both decisions use the same predecessor.
3. Force publication order using a second latch to reproduce both A→B and B→A interleavings, with bounded waits and recorded traces.
4. Exercise preparation/cancellation, cancellation/payment local milestone, repeated cancellation, serial controls and an independent-identity control.
5. Assert counterexample outputs intentionally: final history can lose an acknowledged prior fact, including terminal cancellation. The tests pass when the negative behavior is accurately reproduced.
6. Preserve production `OrderWorkflowService` and `InMemoryOrderRepository` unchanged. All barriers and scheduling belong to the test repository wrapper.
7. Record observed versus inferred behavior and preserve this **fragile checkpoint** for later fixes.
8. No `synchronized` in production, optimistic version, compare-and-set repository contract, PostgreSQL lock, database transaction, distributed lock, Redis, Kafka or Saga in P13.

## Alternatives

- **Random Thread.sleep races**: rejected; brittle, scheduling luck, inconclusive failures and CI flakiness.
- **Only sequential fabricated stale successors**: already tested in P11, useful but insufficient for P13's real two-thread overlap target.
- **Test-only barriers/latches (selected)**: deterministic capture of old state and forced writer order, reproducible assertion of causal chain.
- **Production synchronized method**: premature and cannot coordinate multiple JVMs; would conceal the failure pressure that the roadmap wants to study.
- **Atomic CAS/versioned save**: a possible later solution after specifying authoritative version, conflict/retry semantics and transactional medium; deliberately deferred.
- **Serializable SQL transaction or SELECT FOR UPDATE**: no SQL exists and P13 forbids premature database locking.
- **Distributed locking or service coordination**: unjustified for one in-memory application and cannot automatically fix external authority/commit uncertainty.

## Cost and correctness consequences

The harness adds JUnit complexity, coordination and timed waits; if an assertion or guard changes, a *negative* test may need review because its aim is to preserve a known counterexample rather than enforce a desired production guarantee.

A locally P10-valid final Order does not ensure a globally acceptable history. A real fix must enforce **one authoritative acceptance sequence for a given OrderId**, under specified concurrency, failure and storage semantics, then demonstrate the old P13 counterexamples no longer occur in protected paths.

## Reversal triggers

Introduce stronger protection only in the planned consistency/persistence evolution when evidence and business invariants justify it. Test under both serial and adversarial schedules; demonstrate no lost acknowledged facts, correct conflict classification, and concurrency behavior across the intended process/store boundary.
