# C1.1-P11 Experiment — Logical Atomicity and Local Consistency

## Question

Which related changes must be treated as one logical decision, and what happens if a failure occurs between independently committed representations of that decision?

## Hypothesis

For completion:

state = COMPLETED
and
history contains ORDER_COMPLETED

belong to one logical evolution.

If they are split into independent writes, a failure after the first write will expose invariant-invalid partial state regardless of write ordering.

The current immutable Order path should instead construct one complete invariant-valid representation before repository publication.

## Predictions

1. normal completion returns a COMPLETED Order containing ORDER_COMPLETED;
2. the original PREPARING Order remains unchanged;
3. status-first split write + injected failure violates the COMPLETED-state invariant;
4. history-first split write + injected failure violates the completion-fact invariant;
5. performing both split writes restores consistency;
6. repository failure before replacement leaves old authoritative PREPARING Order unchanged;
7. successful replacement exposes a complete COMPLETED representation;
8. none of these observations prove database transaction, durability, or concurrent read-modify-write atomicity.

## Controlled variables

The domain business decision is always completion of the same valid PREPARING Order.

Only the publication/storage strategy changes:

- one immutable OrderEvolution path;
- split status/history writes;
- repository failure before replacement.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P11 CI.

## Limitations

Not tested:
- real DB commit/rollback;
- process crash durability;
- concurrent competing writers;
- multiple Orders/aggregates;
- network participants;
- response-lost retry semantics.

## Conclusion

Pending execution evidence.
