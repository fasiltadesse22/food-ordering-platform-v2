# C1.1-P18 Experiment — Process Restart and Durability

## Question

Does an Order acknowledged and observable in one real Spring Boot JVM survive termination of that JVM and startup of a fresh process using the same application artifact?

## Hypothesis

No.

InMemoryOrderRepository stores Order state only in a ConcurrentHashMap owned by process memory.

A fresh JVM constructs a new empty repository.

## Execution

GitHub Actions run:
38035108764

Commands executed by CI:

mvn -B -ntp verify

experiments/cluster-1.1/p18-process-restart-durability.sh

## Root verification observation

- tests run: 86
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

## Real process-boundary observation

Process A:
- started as a real java -jar Spring Boot process;
- placed an Order through HTTP;
- read the Order successfully;
- cancelled the Order through HTTP;
- observed OrderId e6dbb2bd-0a9c-47c0-ad9b-f57681b4b0be as CANCELLED with HTTP 200.

Crash:
- process A PID 2504 was terminated with SIGKILL.

Process B:
- started as a new java -jar Spring Boot process from the same built artifact;
- queried the exact same OrderId;
- returned HTTP 404.

Observed log summary:

P18 before crash:
orderId=e6dbb2bd-0a9c-47c0-ad9b-f57681b4b0be
status=CANCELLED
http=200

P18 process terminated with SIGKILL:
pid=2504

P18 after restart:
orderId=e6dbb2bd-0a9c-47c0-ad9b-f57681b4b0be
http=404

## Controlled variables

Unchanged between process A and B:
- application artifact;
- application code;
- repository implementation;
- HTTP contract;
- OrderId queried;
- configured port.

Changed:
- JVM/process identity;
- process-owned memory.

## Evidence interpretation

Executed and verified:
- the Order existed and was observable as CANCELLED before process loss;
- process A was forcibly terminated;
- a new process started successfully;
- the prior Order could not be recovered from the current application and returned 404.

Structurally demonstrated:
- InMemoryOrderRepository stores state only in its ConcurrentHashMap field;
- no startup recovery source exists.

Evidence-backed inference:
- the current Order authority is transient across JVM replacement;
- successful local save and successful HTTP acknowledgement do not currently imply restart durability;
- restoring application execution capacity does not restore acknowledged business state.

## Important evidence boundary

The experiment demonstrates non-durability for the current in-memory adapter under the tested full JVM replacement.

It does not establish behavior for:
- PostgreSQL;
- database WAL/crash recovery;
- filesystem persistence;
- graceful shutdown persistence;
- container/Kubernetes restart policies;
- replication;
- backup/restore;
- disaster recovery.

## Conclusion

The current in-memory Order authority is not a durable recovery source.

This failure is preserved as evidence for Cluster 1.2 rather than fixed in P18.
