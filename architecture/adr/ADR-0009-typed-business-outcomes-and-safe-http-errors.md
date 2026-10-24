# ADR-0009 — Expected business outcomes are values; technical failures remain exceptional

**Status:** Accepted provisionally, independent V8.0 P09 lineage.

## Context

Previously `OrderWorkflowService` returned `OrderSnapshot` on accepted transitions and threw `InvalidOrderTransitionException` for forbidden operations. A controller could mistakenly treat all runtime exceptions alike. The old broad `IllegalArgumentException/NullPointerException → 400` advice also risked calling an internal programming failure invalid client input.

Part 1.1.9 requires business refusal, technical error, application result and HTTP translation to be unambiguous and testable, without retry/circuit-breaker mechanisms.

## Decision

1. Add sealed `OrderWorkflowResult` with `Accepted(OrderSnapshot)` and `Rejected(BusinessRejection(Code,action,message))`.
2. Add `OrderWorkflowDecisionUseCase.execute`; implement within the existing `OrderWorkflowService`, delegating to its seven existing guarded actions. **Only** `InvalidOrderTransitionException` becomes typed `Rejected`; missing Order, contract defects, repository failures and unforeseen exceptions propagate.
3. Keep older workflow use case methods and prior `PlaceOrderResult` contract intact for regression continuity; no unnecessary broad rewrite.
4. Add a **learning-only** workflow action HTTP controller that translates `Accepted → 200` and `Rejected → 409` stable business code. `RESTAURANT_REJECTED` is successful business action and remains 200.
5. Distinguish explicit malformed path/request errors `400 INVALID_INPUT`, missing Order `404 NOT_FOUND`, and unexpected defects `500 TECHNICAL_FAILURE`. Sanitize technical messages. Remove broad generic `IllegalArgumentException/NullPointerException → 400` mapping.
6. Retain existing placement endpoint's `201` and `422` semantics, since endpoint-specific domain contracts differ.
7. Do **not** add retry, circuit breaker, fallback, distributed transaction, new persistence, payment provider or production authentication/authorization.

## Alternatives and rationale

- **Catch all exceptions as business rejection:** rejected; hides infrastructure and programming defects, corrupting client and SLI meaning.
- **Return 500 for every rejected Order:** rejected; valid policy denial isn't necessarily unavailable service.
- **Return 409 for RESTAURANT_REJECTED accepted action:** rejected; confuses negative business outcome with failed processing.
- **Use exception message as sole API contract:** rejected; brittle for callers; stable business codes preferred.
- **Use one universal HTTP code for all non-success:** rejected; loses input/missing/business/technical distinctions.
- **Add retries or circuit breakers now:** rejected; V8 P09 explicitly preserves lack of resilience mechanisms and current repo has no remote dependency requiring them.
- **Refactor every existing Java method:** rejected; breaks V8 earlier checkpoint continuity without need. New typed boundary is explicit and tested.

## Limits and unresolved risks

The newly added action endpoint has **no actor authentication/authorization** and is not production-ready. Current repository is in-memory with no durability, multi-process consistency or reliable unknown-outcome resolution. No production dashboard/tracing/metric counters are implemented: this ADR defines future dimensions without claiming observed SLIs. A 409 is an appropriate technical response but may still be a customer problem at high volumes.
