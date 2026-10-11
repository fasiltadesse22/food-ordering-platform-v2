# ADR-0023 — Order as Provisional Local Aggregate Root Candidate

Status: PROVISIONAL design recommendation; accepted for C1.2-P04 learning scope, not final aggregate schema or guarantee.
Inherited checkpoint: checkpoints/C1.2-P03 (55c6169194ebbc19aae437462ec1a7358a772f3d).

## Decision
Model one continuing Order identity as the candidate immediate consistency unit, with the existing Order domain class as prospective aggregate root. Retain its ownership of local lifecycle status, lines, relevant workflow occurrences, associated identity references and placement time. The complete Customer account, Restaurant model/menu/inventory and external payment-provider effects do not become owned parts simply because they are related or referenced.

## Drivers
P01 derived invariant-to-state dependencies; P02 established identity != object instance or current authority; P03 established candidate != commit; C1.1-P11 demonstrated invalid status/history partial states; P13/P14 conflicting acknowledgments, P17 stale state loss, P18 JVM durability failure. Existing normal Order methods centralize decisions and selected invariant checks.

## Alternatives
- Independent status/history aggregates: increase immediate coordination pressure and may expose partial commits. Not justified.
- Large all-related-data aggregate: increases coupling; cannot capture external payment commit. Not justified.
- Future-service-name aggregate design: mixes semantic and operational boundaries. Not justified.
- Event-sourced authoritative history: changes state model and requires new evidence. Not justified.

## Tradeoffs and unresolved rules
Chosen candidate lowers current domain coordination complexity. Does not prove best aggregate size under production workload, complete history validation, conflict resolution, local durable transactions, external consistency, or actor authentication.
Payment after cancellation, modification after payment, cancellation/acceptance precedence and complete workflow-history semantics remain open.

## No premature mechanism
No production code, database schema, version field, SQL transaction, new aggregate module, Kafka, Saga or deployment split. P05/P06 refine entity/value semantics; P07 root encapsulation, P11–P21 evaluate real persistence, conflict, size and cross-aggregate effects.

## Review triggers
A changed invariant crossing authorities, evidence of false ownership, repeated cross-aggregate atomic updates, measurable hot-aggregate contention, growing history update amplification, independent domain lifecycles, or changed authoritative history requirements.
