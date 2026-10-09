# Food Ordering Platform — V8.0 Fresh-Start Evolution

Inherited verified checkpoint: C1.1-P11
Current evolution: C1.1-P12 candidate — Requirement, Implementation, Observation & Guarantee

## Part type

Type A — conceptual + evidence evolution.

P12 intentionally does not change production application behavior.

## Verification

mvn -B -ntp verify

The exact P12 evidence checkpoint is not frozen until CI executes successfully.

## Evidence vocabulary

The project now distinguishes:
- requirement;
- implementation;
- observation;
- evidence-backed inference;
- assumption;
- scoped guarantee/non-guarantee.

## Critical rule

A passing test is an observation about an exercised scenario.

It is not, by itself, proof of every possible behavior.

A scoped guarantee must identify:
- property;
- boundary;
- enforcing mechanism;
- assumptions;
- evidence;
- exclusions.

## Current unchanged topology

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- no PostgreSQL;
- no @Transactional;
- no messaging;
- no concurrency control.

See:
- architecture/evidence/evidence-language.md
- architecture/evidence/C1.1-P12-claim-matrix.md
- architecture/adr/ADR-0012-evidence-language-and-claim-strength.md
