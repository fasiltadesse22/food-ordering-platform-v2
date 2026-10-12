# ADR-0029 — Separate New Order Creation from Unconditional Successor Publication

Status: Accepted for C1.2-P10 only, subject to CI gate.
Base verified P09 commit 000c2960fdbbe85106fc68c051193f5fafc37675.

## Context
InMemoryOrderRepository.saveCurrent uses ConcurrentHashMap.put, so placement of a supposedly new Order could replace an existing Order with equal OrderId. Order identity continuity and historical fact semantics require creation not to erase an existing entity. Independent issue: stale existing-Order successors can also overwrite current authority; P17 before-state evidence must remain observable until P16–P18.

## Decision
Add `OrderRepository.createNew(Order)`, backed by atomic in-process `putIfAbsent`; throw typed `OrderAlreadyExistsException` on collision without replacement. Update PlaceOrderService to use createNew before OrderPlaced.from. Explicitly document saveCurrent as legacy unsafe unconditional publication and continue using it for existing Order transitions.
Preserve present single-deployable/in-memory topology and all P13/P14/P15/P16/P17/P18 before-state experiments. Adapt test-only repository implementations to the new port intentionally (unsupported create where no creation is tested; preserve P09 before-/after-publication fault injection through createNew).

## Why now?
New entity creation versus current-state replacement is a fundamental domain repository semantic with a real confirmed weakness. Atomic absent-key creation is justified without selecting a database/ORM or designing versioned successor commits ahead of schedule.

## Alternatives
- Existing `put` API for both semantics: false replacement risk.
- Pre-check then `put`: race between absent-key check and write.
- Lock or version entire repository now: premature and hides P17.
- Persist SQL now: reserved for P11/12.
- Silent UUID regeneration on conflict: hides behavior and lacks explicit retry policy.
- Convert random ID collision to business actor rejection: misclassifies an infrastructure identity conflict.

## Consequences and limits
New create semantic prevents same-key replacement in one process. Does not prevent two creations with separate IDs from the same retried intent; no command idempotency.
Current find optional empty is scoped to this repository instance; not proof of never-created or globally absent.
saveCurrent can still upsert absent ID and overwrite stale successor. Keep and measure before later version/transaction evolution.
No durable representation/reconstitution/DB transaction/replication/cross-aggregate authority now.
