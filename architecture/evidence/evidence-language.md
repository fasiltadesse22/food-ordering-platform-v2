# Evidence Language — Course 2

This file defines the evidence vocabulary for the evolving Food Ordering Platform.

## Claim classes

| Class | Core question | Valid evidence examples | What it must not be confused with |
|---|---|---|---|
| Requirement | What must/should be true? | business rule, roadmap, ADR decision | implementation or observed behavior |
| Implementation | What is currently built? | source/config/schema inspection | universal runtime guarantee |
| Observation | What happened in a specific execution? | test/CI/log/metric/trace/experiment result | all possible behavior |
| Evidence-backed inference | What conclusion is supported but not directly observed? | observation + mechanism reasoning + assumptions | direct measurement |
| Assumption | What premise are we currently relying on? | explicit reasoning premise | fact |
| Scoped guarantee | What property is enforced, where, and under what assumptions? | mechanism + bounded scope + evidence | global/unqualified guarantee |
| Non-guarantee | What property has not been established? | missing mechanism/evidence or explicit fragile state | failure claim unless executed |

## Mandatory claim template for strong architectural claims

When a claim uses language such as "guarantees", record:

Property:
Scope:
Mechanism:
Assumptions:
Evidence:
Exclusions/non-guarantees:

Example:

Property:
successful construction through the current Order evolution path produces an Order whose selected state/history invariants pass.

Scope:
the current in-process domain construction path.

Mechanism:
private Order constructor + OrderInvariants.verify + immutable replacement.

Assumptions:
callers use the current public domain methods; constructor is not bypassed by unsupported mechanisms.

Evidence:
Order invariant tests and P11 atomicity tests in the verified checkpoint.

Exclusions:
durability, concurrent read-modify-write, database constraints, distributed state.

## Evidence verbs

Use precisely:

Observed
- directly seen in a named execution.

Measured
- numerically observed with units under stated conditions.

Executed and verified
- required executable check ran and satisfied its expected assertion.

Structurally demonstrated
- visible from repository source/configuration/artifact structure, without claiming runtime proof.

Evidence-backed inference
- reasoned conclusion from evidence plus mechanism knowledge.

Assumed
- premise not established by the current evidence.

Hypothetical
- scenario used for reasoning but not executed.

Not yet verified
- implementation may exist, but required execution evidence is absent.

Not guaranteed
- current mechanisms/evidence do not establish the property.

## Anti-pattern language

Do not write:
- "CI proves the system is correct."
- "The use of ConcurrentHashMap makes Order updates atomic."
- "The invariant can never be violated."
- "The service is production-ready."
- "Database transactions will solve consistency."
- "The operation is idempotent."
- "Kafka guarantees exactly once."
- "The service is highly available."

unless the exact claim is narrowed and supported.

## Falsifiability rule

Every important architectural claim should answer:

What observation would prove this claim wrong?

If no possible evidence could change the decision, the claim is not being treated scientifically enough for this course.
