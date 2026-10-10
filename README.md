# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current stage: C1.1-P19 / C1.1-FINAL evidence-bearing freeze candidate
Inherited verified checkpoint: C1.1-P18

## P19

Correctness Synthesis, Diagnosis, Design Defense and Cluster Freeze.

Engineering question:

Can we prove, explain, diagnose and defend what the current system guarantees—and what it does not?

## Project evolution

Type A only.

Production behavior remains unchanged.

The final Cluster 1.1 review package is under:
architecture/cluster-1.1/

## Candidate verification

P19 candidate commit:
a2f1360dc0a52327300b561fa3900bae60ae1f6f

GitHub Actions:
38035664742

Observed:
- 86 Maven tests passed;
- 0 failures;
- 0 errors;
- BUILD SUCCESS;
- P18 real JVM-replacement experiment reproduced CANCELLED-before-kill / 404-after-restart.

## Final baseline architecture

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- executable lifecycle/guard/invariant model;
- preserved concurrency, replay, external-effect, stale-state and durability failure evidence.

No PostgreSQL or later distributed mechanisms are added in Cluster 1.1.

## Cluster 1.2 handoff

Cluster 1.2 must inherit the final verified C1.1 commit exactly.

It may then evolve:
- durable relational persistence;
- aggregate/consistency boundaries;
- local transactions;
- selected DB constraints;
- optimistic concurrency implications;

only from the evidence preserved here.

The evidence-bearing commit still requires its own CI pass before C1.1-P19 and C1.1-FINAL branches are created.
