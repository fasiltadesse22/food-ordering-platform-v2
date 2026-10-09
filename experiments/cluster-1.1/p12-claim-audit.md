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

Classification:
unsupported overclaim.

Corrected:
"Individual map operations are provided by ConcurrentHashMap, but the application-level find→evolve→save sequence is not established as atomic."

### Example B

Candidate:
"P11 proves database transactions are required."

Classification:
too strong.

Corrected:
"P11 provides evidence that independently committed representations of the selected state/history pair create partial-state failure windows. If a future persistence design stores them independently while preserving the same immediate invariant, an atomic commit boundary or equivalent mechanism is required."

### Example C

Candidate:
"A repository exception means the Order was not saved."

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
"GitHub Actions run 37902228783 executed 68 tests with zero failures for the exercised current checkpoint scenarios. This is an observation about those paths, not universal proof of correctness."

## Execution

GitHub Actions run:
37902228783

Command:
mvn -B -ntp verify

Observed:
- tests run: 68;
- failures: 0;
- errors: 0;
- skipped: 0;
- BUILD SUCCESS.

## Interpretation

The P12 repository/documentation evolution did not change production code or configuration.

The exact evidence-bearing candidate retained the green P11 application behavior under the existing automated suite.

The CI result is itself classified as an observation.

It does not establish:
- all possible inputs;
- all thread interleavings;
- crash durability;
- production-scale performance;
- availability;
- security completeness;
- future database behavior.

## Limitations

P12 does not:
- create new runtime coverage for concurrency;
- prove durability;
- introduce production telemetry;
- prove future database behavior;
- convert confidence into mathematical proof.

## Conclusion

P12 successfully establishes a disciplined claim vocabulary and applies it to the actual accumulated C1.1 evidence without inventing a new runtime mechanism.
