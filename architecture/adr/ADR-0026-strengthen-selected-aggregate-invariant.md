# ADR-0026 — Strengthen Selected Order Aggregate Invariant Without Premature Mechanisms

Status: Accepted for C1.2-P07.
Base: verified checkpoints/C1.2-P06 `edde75f6dc63cb4c1568939497ab7cea218c5759`.

## Context
P01–P04 derived Order's immediate consistency boundary; P05 established continuing OrderId distinct from Java reference, P06 tested immutable value/collection semantics. C1.1-P11 proved partial status/history contradictions; P13/P14/P17 and P18 preserve concurrency and durability failures.
Source inspection revealed that OrderInvariants verifies seven selected implications but does not reject RESTAURANT_ACCEPTED in PLACED, CANCELLED, or REJECTED. Public transition paths cannot normally produce this, but a future reconstitution or defective alternate construction could. No comprehensive event history or payment deduplication contract exists.

## Decision
Retain Order as the candidate aggregate root with private constructor, business methods, defensive copying and constructor invariant verification.
Add **one targeted accepted-fact consistency check**, matching the current lifecycle: an acceptance occurrence requires current status ACCEPTED, PREPARING or COMPLETED. Add a specific typed violation code and regression tests.
Use controlled tests for public operation guards, immutable state, valid progression and payment-fact replay negative control.
Do not change recordPayment duplicates, actor authentication, transaction/repository semantics, full-history validation or external Payment authority.

## Why this rule and not others?
The current transition enum allows PLACED->ACCEPTED->PREPARING->COMPLETED, with no cancellation/rejection after acceptance. Thus acceptance occurrence paired with PLACED/CANCELLED/REJECTED is contradictory under this currently established policy.
Duplicate payment milestones and timestamp/participant semantics need independently justified rules, not assumptions. Preserve as explicit non-guarantees.

## Alternatives
No fix: representation checker knowingly accepts contradicted accepted history; weakens future reconstruction.
Generic setters: undermines invariants; reject.
Comprehensive event-chain validator now: adds unrequired ordering/deduplication model; premature before P09.
Database constraints, JPA, locks, versioning, Kafka, Saga, service split: irrelevant or premature for the local gap.

## Risks and revisitation
If cancellation after acceptance is later permitted, the new status set must evolve with the state machine and invariant contract.
A narrow implication is not proof of valid chronological event sequence.
Persistence reconstitution must not bypass constructor enforcement.
Performance is unmeasured; scanning histories incurs O(n) work per construction with current list-based checks, to evaluate if future evidence warrants change.

## Evidence and gate
New 7 targeted tests; existing suite plus P18 process restart through GitHub Actions on exact P07 commit.
Freeze checkpoints/C1.2-P07 only after successful exact SHA verification.
