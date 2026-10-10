# Checkpoint Manifest — C1.1-P18 Candidate

## Identity

- Target checkpoint: C1.1-P18
- Current status: CANDIDATE — NOT FROZEN
- Inherited checkpoint: C1.1-P17
- Inherited branch: checkpoints/C1.1-P17
- Inherited commit: 880300e71e44ebf501adac5c6a18aab7389aea5d

## Engineering question

What happens to our correctness claims when the process that currently owns Order state disappears?

## Part classification

Type C — Process-Loss / Durability Failure Evolution.

## Production code/config

Production application behavior remains unchanged.

CI gains a real two-process restart experiment.

## Controlled runtime experiment

Process A:
- start real Spring Boot jar;
- place Order over HTTP;
- GET confirms PLACED;
- cancel Order over HTTP;
- response confirms CANCELLED.

Failure:
- terminate process A with SIGKILL.

Process B:
- start the same jar fresh;
- GET the same OrderId;
- expected HTTP 404.

## Required distinctions

authoritative now != durable.

save != persistent commit.

successful response != restart durability.

thread-safe map != persistence.

process recovery != business-state recovery.

availability != durability.

durability != backup.

HA != DR.

## Mechanisms deliberately absent

- PostgreSQL;
- database transaction semantics;
- optimistic locking;
- durable cache/store;
- replication;
- backups;
- Kafka;
- Saga;
- Outbox;
- distributed transaction;
- Kubernetes/AWS HA mechanisms.

## Freeze gate

1. mvn -B -ntp verify succeeds;
2. inherited P01-P17 tests remain green;
3. real process A starts and serves HTTP;
4. Order is acknowledged and observed as CANCELLED before crash;
5. process A is terminated with SIGKILL;
6. fresh process B starts from the same artifact;
7. GET for prior OrderId returns 404;
8. exact runtime evidence is recorded;
9. evidence-bearing commit re-verifies before checkpoint branch creation.
