# Checkpoint Manifest — C1.1-P18

## Identity

- Checkpoint: C1.1-P18
- Current status: VERIFIED AND FROZEN
- Inherited checkpoint: C1.1-P17
- Inherited branch: checkpoints/C1.1-P17
- Inherited commit: 880300e71e44ebf501adac5c6a18aab7389aea5d
- P18 experiment commit: 9d462abdc089e51d8cb7abd014226b3fc999ce1c
- P18 verification run: 38035108764

## Engineering question

What happens to our correctness claims when the process that currently owns Order state disappears?

## Part classification

Type C — Process-Loss / Durability Failure Evolution.

## Production code/config

Production application behavior remains unchanged.

CI now executes the real two-process restart experiment after the root Maven build.

## Verified root gate

mvn -B -ntp verify

Observed:
Tests run: 86
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

## Verified runtime experiment

Process A:
- placed and read an Order;
- cancelled it;
- observed OrderId e6dbb2bd-0a9c-47c0-ad9b-f57681b4b0be as CANCELLED over HTTP 200.

Failure:
- process A PID 2504 terminated with SIGKILL.

Process B:
- same application artifact started in a fresh JVM;
- GET same OrderId returned HTTP 404.

## Verified distinctions

authoritative now != durable.

saveCurrent != persistent durable commit.

successful HTTP response != restart durability.

ConcurrentHashMap thread safety != persistence.

process recovery != business-state recovery.

application availability != state durability.

durability != backup.

HA != DR.

## Mechanisms deliberately absent

- PostgreSQL;
- database transaction/crash recovery;
- optimistic locking;
- durable cache/store;
- replication;
- backup/restore;
- Kafka;
- Saga;
- Outbox;
- distributed transactions;
- Kubernetes/AWS HA mechanisms.

## Forward boundary

Part 1.1.19:
Correctness Synthesis, Diagnosis, Design Defense and Cluster Freeze.

Cluster 1.2:
durable relational persistence and aggregate/consistency-boundary evolution.
