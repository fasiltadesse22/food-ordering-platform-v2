# Checkpoint Manifest — C1.2-P03 Candidate

## Identity
Part: 1.2.3 Atomicity Requirements and the Shape of a Consistency Boundary
Inherited: `checkpoints/C1.2-P02` at `e6919e51a5deb5b105397b44205153f1375eab73`
Evolution: A/C documentation and controlled in-process failure tests; production runtime unchanged.
Checkpoint candidate status before CI: NOT YET VERIFIED.

## Engineering question
When must multiple changes become authoritative together, and why is constructing a coherent domain successor not sufficient to prove an atomic committed update?

## Changes
Four targeted tests: candidate-before-save, failure-before-publish, publish-then-exception, domain rejection leaves save untouched.
Scenario atomicity matrix, ADR-0022, experiment plan, evidence ledger, this manifest.
No database, schema, @Transactional, lock, version field, external authority, Kafka, or service split.

## Required verification
On P03 commit, GitHub Actions must run root `mvn -B -ntp verify` and
`experiments/cluster-1.1/p18-process-restart-durability.sh`.
Prior P02 CI success is inherited evidence but does not substitute for execution of changed tests.

## Preserved fragile behavior
Concurrent contradictory Accepted calls P13/P14; stale replacement P17; process loss P18;
replay P15; external payment effects P16; no complete history validator or legal business winner policy.

## Proposed and deferred implementation
P15 physical local transaction and rollback tests after P11/P12 PostgreSQL; P16/P17 concurrency/isolation;
P18 commit-unknown/client retry semantics; P21 cross-authority analysis.

## Next
P04 derive aggregate candidates from invariants, preserving provisional Order-centered boundary.
