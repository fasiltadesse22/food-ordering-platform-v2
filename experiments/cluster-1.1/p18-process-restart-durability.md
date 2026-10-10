# C1.1-P18 Experiment — Process Restart and Durability

## Question

Does an Order acknowledged and observable in one real Spring Boot JVM survive termination of that JVM and startup of a fresh process using the same application artifact?

## Hypothesis

No.

InMemoryOrderRepository stores Order state only in a ConcurrentHashMap owned by process memory.

A fresh JVM constructs a new empty repository.

## Prediction

Process A:
- application starts;
- Order is placed through HTTP;
- Order can be read;
- Order is cancelled;
- GET returns CANCELLED.

Crash:
- process A receives SIGKILL.

Process B:
- same jar starts on the same port;
- GET for the prior OrderId returns HTTP 404.

## Setup

The normal root Maven verification builds the executable Spring Boot jar.

CI then runs:
experiments/cluster-1.1/p18-process-restart-durability.sh

The script starts two real java -jar processes sequentially.

## Controlled variables

Unchanged between process A and B:
- repository code;
- application jar;
- HTTP contract;
- OrderId queried;
- runtime configuration except process identity.

Changed:
- JVM/process identity and therefore process memory.

## Execution

1. mvn -B -ntp verify
2. experiments/cluster-1.1/p18-process-restart-durability.sh

## Observation

Pending P18 CI.

## Evidence interpretation

If process A returns CANCELLED before crash and process B returns 404 afterward:
- current in-process authority is transient;
- successful local save/HTTP response is not a durability guarantee;
- application restart restores compute but not acknowledged Order state.

## Limitations

This experiment does not test:
- PostgreSQL;
- filesystem persistence;
- graceful shutdown persistence;
- container/Kubernetes restart semantics;
- database crash recovery;
- replication;
- backup/restore;
- RPO/RTO;
- HA/DR.

## Conclusion

Pending exact P18 execution evidence.
