# Checkpoint Manifest — C1.1-P12 Candidate

## Identity

- Target checkpoint: C1.1-P12
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P11
- Inherited branch: checkpoints/C1.1-P11
- Inherited commit: f68c1fa3b23075e112a15a5bf3828bb083a48711

## Engineering question

What exactly do we know, how do we know it, and how strong a claim are we justified in making?

## Part classification

Type A — Conceptual + Evidence Evolution.

No production runtime behavior is changed.

## Evidence vocabulary

- Requirement
- Implementation statement
- Observation
- Evidence-backed inference
- Assumption
- Scoped guarantee
- Non-guarantee

## Required distinctions

requirement != implementation.

implementation != observation.

observation != universal guarantee.

test pass != proof of all behavior.

source inspection != runtime proof.

evidence-backed inference != direct observation.

assumption != fact.

documented rule != implemented guarantee != experimentally verified behavior.

## Freeze gate

1. P12 evidence vocabulary and claim matrix are committed;
2. representative P01-P11 claims are reclassified with explicit scope;
3. unsupported production-ready/concurrency/durability claims are rejected;
4. root Maven verification executes successfully on the exact P12 evidence-bearing commit;
5. fragile states required by P13+ remain unchanged.
