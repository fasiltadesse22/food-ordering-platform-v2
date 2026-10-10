# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current stage: C1.1-P19 / C1.1-FINAL final freeze annotation
Inherited verified checkpoint: C1.1-P18

## Cluster 1.1 final part

Correctness Synthesis, Diagnosis, Design Defense and Cluster Freeze.

Engineering question:

Can we prove, explain, diagnose and defend what the current system guarantees—and what it does not?

## Project evolution

Type A only.

Production behavior is unchanged.

Final Cluster 1.1 review package:
architecture/cluster-1.1/

## Verified P19 evidence

Synthesis candidate:
a2f1360dc0a52327300b561fa3900bae60ae1f6f
GitHub Actions 38035664742

Evidence-bearing commit:
015b6c2c465796882c914d4c5bee53d9266468f9
GitHub Actions 38035744146

Both observed:
- 86 Maven tests passed;
- BUILD SUCCESS;
- P18 real JVM-replacement experiment reproduced CANCELLED-before-kill / 404-after-restart.

## Final C1.1 architecture

- one Java 21 / Spring Boot deployable;
- in-memory Order authority;
- executable lifecycle/guard/invariant model;
- evidence-backed failure artifacts for concurrency, replay, external payment effects, stale state and durability.

No PostgreSQL or later distributed mechanisms have been introduced.

## Cluster 1.2 handoff

Cluster 1.2 must inherit C1.1-FINAL exactly.

It may then derive/evolve:
- entities/value objects/aggregate boundary;
- durable PostgreSQL persistence;
- local transaction boundary;
- selected DB constraints;
- optimistic concurrency implications;

from the evidence preserved here.

The final metadata-only freeze commit is reverified before immutable checkpoint branches are created.
