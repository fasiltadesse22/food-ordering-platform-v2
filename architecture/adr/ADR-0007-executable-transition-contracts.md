# ADR-0007 — Executable Preconditions and Postconditions

**Status:** Accepted provisionally. **Scope:** newly authored V8.0 Part 1.1.7 on `redo/v8-c1.1-p07`.

## Problem
P06 established allowed lifecycle edges but Part 1.1.7 requires a falsifiable distinction between preconditions, postconditions and business rejection. The team must prove why one guard matters rather than only demonstrating that an if-statement exists.

## Decision
- Keep the exact P06 business rules; no payment/cancellation policy expansion.
- Extract checks to package-local `OrderTransitionContract.requirePreconditions`: reject forbidden sources or missing recorded prerequisites without returning a successor.
- Construct a new immutable Order only on guard success.
- Verify `OrderTransitionContract.requirePostconditions` before returning the successor to the application service: stable identity and business contents; expected status; full old occurrence prefix; exactly one new attributed, timestamped occurrence.
- Use `InvalidOrderTransitionException` for expected business denial and `OrderTransitionContractViolationException` for a defective accepted successor. Neither is a provider/network error.
- Keep `OrderWorkflowService.findCurrentById → domain transition → saveCurrent`. A failed domain check never reaches save in the current normal service path.

## Alternatives and costs
Controller-only guard checks can be bypassed by non-HTTP callers. Application-only guards scatter lifecycle policy. Java `assert` may be disabled and is not sufficient enforcement. Postconditions after repository save catch a mistake too late. A heavy contract/state-machine framework is unnecessary for this small model. Transaction isolation and locking belong to later V8 stages.

## Controlled experimental evidence
An isolated experiment branch removes one guard. Existing P06/P07 tests detect it, as shown in the accompanying evidence ledger. That intentionally failing experiment branch is never a release or predecessor baseline.

## Limitations
Pre/post verification is sequential and local, not proof of concurrency serializability. No actual provider transaction, refund execution, authenticated workflow-action API, durable storage or crash recovery. The preparation payment prerequisite is a provisional local milestone, not external capture. Cancelled status does not physically halt restaurant work.

## Forward dependency
P08–P12 reason about reversibility, business invariants and stronger atomicity/evidence. P13–P17 reproduce stale-state races and justify stronger consistency mechanics.
