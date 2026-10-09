# C1.1-P12 Experiment — Evidence Claim Audit

## Question

Can we classify representative C1.1 project statements without confusing requirement, implementation, observation, inference, assumption and guarantee?

## Hypothesis

If evidence discipline is correct:
- several statements that sound like "guarantees" must be weakened;
- a passing CI run will classify as an observation, not universal proof;
- deterministic injected failures can justify specific negative conclusions;
- future mechanisms such as PostgreSQL transactions remain inference/hypothesis until implemented and executed.

## Setup

Inputs:
- P01-P11 evidence ledgers;
- current source tree;
- current tests;
- verified GitHub Actions runs;
- preserved non-guarantees.

Controlled variable:
the wording/strength of each claim.

No production behavior is changed.

## Audit examples

### Example A

Candidate:
"ConcurrentHashMap makes Order workflow updates atomic."

Evidence:
repository uses ConcurrentHashMap.

Classification:
unsupported overclaim.

Corrected:
"Individual map operations are provided by ConcurrentHashMap, but the application-level find→evolve→save sequence is not established as atomic."

### Example B

Candidate:
"P11 proves database transactions are required."

Evidence:
split mutable representations become invariant-invalid after injected failure.

Classification:
too strong.

Corrected:
"P11 provides evidence that independently committed representations of the selected state/history pair create partial-state failure windows. If a future persistence design stores them independently while preserving the same immediate invariant, an atomic commit boundary or equivalent mechanism is required."

### Example C

Candidate:
"A repository exception means the Order was not saved."

Evidence:
one deterministic fake repository throws before replacement.

Classification:
overgeneralization.

Corrected:
"In the P11 pre-replacement failure harness, the injected exception occurred before authority replacement and the old Order remained authoritative. Real remote persistence failures may have ambiguous commit outcomes and are not yet tested."

### Example D

Candidate:
"The application is correct because 68 tests pass."

Classification:
unsupported.

Corrected:
"GitHub Actions run 37886764685 executed 68 tests with zero failures for the exercised scenarios. This increases evidence for those paths but does not establish universal correctness."

## Execution

P12 changes documentation/evidence only.

The exact P12 checkpoint must still execute:

mvn -B -ntp verify

This verifies that the inherited real application remains green after the evidence-audit evolution.

## Observation

Pending P12 CI.

## Limitations

P12 does not:
- create new runtime coverage for concurrency;
- prove durability;
- introduce production telemetry;
- prove future database behavior;
- convert probabilistic confidence into mathematical proof.

## Conclusion

Pending exact-checkpoint CI.
