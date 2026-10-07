# ADR-0001 — Start with One Spring Boot Deployable

- Status: Accepted for the V8 fresh-start baseline
- Scope: C1.1-P01
- Nature: course-stage architecture decision, not permanent production topology approval

## Context

The course starts before service boundaries, remote-call costs, data ownership, scaling needs, failure isolation needs, or team/release pressures have been established by evidence.

## Forces

- Keep business behavior observable in one process.
- Avoid manufacturing distributed failure modes before they are needed.
- Preserve a simpler comparison baseline for Cluster 1.4.
- Build a real application from day one rather than a teaching-model-only repository.
- Keep the future topology falsifiable.

## Alternatives

1. One real Spring Boot deployable.
2. Pre-split Order/Payment/Restaurant services.
3. Pure Java teaching model with no real application.

## Decision

Use one real Spring Boot deployable inside one monorepo.

## Evidence

At C1.1-P01 this decision is authorized primarily by the V8 course contract and by the absence of measured forces requiring distribution. Later clusters must produce evidence before the topology changes.

## Consequences

Positive:

- simple execution path;
- low operational complexity;
- business state easier to reason about;
- later distribution costs become observable as a change from a preserved baseline.

Negative:

- no independent scaling/deployment/failure isolation is provided;
- a single process is a single runtime failure domain.

These are not defects to hide at this stage.

## Non-guarantees

This ADR does not claim a monolith is the final production topology.

## Reversal criteria

Revisit only when Cluster 1.4 evidence demonstrates sufficient pressure from ownership, release cadence, scaling, failure isolation, data ownership, or other justified forces.
