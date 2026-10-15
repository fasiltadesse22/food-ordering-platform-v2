# ADR-0037 — Bounded Fresh-State Re-evaluation and Explicit SQL Failure Taxonomy

Status: Accepted C1.2-P18 candidate; exact verified frozen CI determines freeze.
Authoritative source: V8.0 Cluster1.2 P18 scope, inherited P17 SHA 40144edeb245c9739e74aaabcf9e5359d6601ebf.

## Context
P17 rejects stale Order revisions rather than overwriting newer root/lines/history. That only detects conflict. Different post-failure histories require different decisions: compatible local milestone commits may leave acceptance legal; competing restaurant rejection makes acceptance illegal; uncertain COMMIT may have succeeded; 40001/40P01/55P03 are technical database coordination events not ordinary business rejections.

## Decision
1. OrderWorkflowService: after confirmed StaleOrderVersionException, permit exactly one fresh-load/new-domain-evolution/new-CAS attempt ONLY for current local-only lifecycle operations accept/reject/cancel/start preparation/complete. If new state makes command illegal, return existing domain rejection. If second CAS stale, return STALE_ORDER_VERSION. No blind stale-successor resubmission, no automatic long retry loop.
2. recordPayment and recordRefundRequest remain NO_RETRY; ModifyOrderService remains single-attempt to avoid obsolete replacement. PlaceOrderService createNew remains no generic retry as random OrderId can duplicate logical placement.
3. PostgresOrderRepository: precommit SQLState 40001 maps to OrderDatabaseConcurrencyException SERIALIZATION_FAILURE, 40P01 DEADLOCK_DETECTED, 55P03 LOCK_NOT_AVAILABLE. The exception retains OrderId and SQLState; unrecognized SQL failures retain OrderPersistenceException. P15 COMMIT-attempted uncertainty retains OrderCommitOutcomeUnknownException and is never auto replayed.
4. Keep P15 atomic transaction, P16 read snapshot, P17 revision CAS and migrations unchanged. No external Payment effect added.
5. Test actual PostgreSQL compatible and incompatible race, bounded double conflict, no payment/line auto replay, real server 40001 and 55P03 via controlled interleavings, post-commit uncertain-response proxy with a genuine DB commit and no replay.

## Architectural explanation
A retry of the same SQL statement is not the same as retrying a complete database transaction, re-evaluating the domain command, or replaying a logical client request. Version changes prove currentness loss, not operation identity. Failure classification establishes what can be inferred, not automatic retry permission. Even bounded lifecycle re-evaluation is safe only while those commands have purely local effects; revisit on future external integrations.

## Alternatives
Universal automatic retries rejected: provider double charge/Order duplicate, unknown COMMIT, retry storms. Retry same successor rejected: expected revision unchanged. Automatic merge of line modifications rejected: no user-intent merge rules. Retry every status-preserving milestone rejected: business duplicate semantics unresolved. SELECT FOR UPDATE through service transaction considered but increases lock holding/tx scope, doesn't settle ambiguous external effects. Generalized retry framework, distributed locks, Saga or event sourcing rejected as premature.

## Evidence and caveats
Concrete P17 checkpoint shows exactly one CAS winner; P18 must show loser re-evaluates on fresh state. Success under selected deterministic interleavings does not claim general idempotency, provider correctness, HA, response replay, generic 40001 auto retry or throughput. Future policy design requires workload measurements/SLIs and stable operation identity when client repeats must be recognized.
