# C1.1-P10 Experiment — Invariants, Validation and Constraints

## Question

Can the project demonstrate that structurally valid data may still violate a business invariant, while HTTP validation and future database constraints remain separate enforcement layers?

## Hypothesis

If the P10 classification is correct:

1. blank identifiers fail structural validation;
2. non-positive quantity and negative price fail structural value validation;
3. HTTP malformed requests fail before the use case is invoked;
4. structurally valid lifecycle facts can still form an invariant-invalid state/history combination;
5. public Order behavior rejects CANCELLED -> PREPARING before constructing an invalid Order;
6. valid compensation history remains invariant-valid even though CANCELLED is terminal;
7. no PostgreSQL/database constraint is required to prove the semantic invariant distinction.

## Controlled variables

Validation experiments vary request/value shape while business lifecycle is irrelevant.

Invariant experiments keep every individual value structurally valid and vary only the relationship among state/history facts.

Database constraints remain absent, preserving the in-memory baseline.

## Execution

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P10 CI.

## Limitations

This experiment does not prove:
- database constraints enforce anything;
- durable state is protected;
- concurrent writes preserve invariants;
- transactions are atomic;
- stale-state decisions are safe.

## Conclusion

Pending execution evidence.
