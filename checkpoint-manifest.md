# Checkpoint Manifest — C1.1-P01

## Identity

- Checkpoint: `C1.1-P01`
- Current status: **VERIFIED AND FROZEN**
- Inherited checkpoint: none
- Baseline: fresh V8.0 repository
- Verified code/config commit: `b91edcd9d3e14b7dbd7ba7cac54cd187a1182625`
- Verification workflow run: `37628171669`

## Engineering question

What must remain correct before we make any architectural decision?

## Prediction before implementation

A minimal real Food Ordering vertical slice can be implemented as one Spring Boot deployable with a framework-independent domain/application core and an in-memory output adapter, while deliberately avoiding premature distributed-system mechanisms.

## Source/config changes

Key elements:

- Maven root reactor
- one Spring Boot application module
- domain layer
- application input/output ports
- application services
- minimal HTTP adapter
- in-memory repository adapter
- tests
- JDK-only core verification harness
- CI workflow
- architecture scenarios/ADRs/evidence

A verification defect discovered by CI was repaired by adding the focused Spring Boot MVC test starter required by Spring Boot 4.1.1.

## Verification

Authoring-environment evidence:

```bash
java -version
./scripts/verify-core.sh
```

Authoritative reproducible verification:

```bash
mvn -B -ntp verify
```

GitHub Actions run `37628171669` executed the root Maven reactor on Java 21 and completed successfully.

Observed test result:

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The successful run includes:

- Spring Boot context load;
- domain tests;
- application-service test;
- MockMvc HTTP integration tests.

## Evidence status

- Java 21: **executed and verified in CI**
- framework-free core compile: **executed and verified**
- framework-free place/get behavior: **executed and verified**
- root Maven reactor: **executed and verified**
- Spring context: **executed and verified**
- HTTP integration: **executed and verified**
- CI: **executed and verified**

## Known fragilities intentionally preserved

- state is transient and lost on restart;
- no database transactions;
- no optimistic concurrency;
- no duplicate-command/idempotency mechanism;
- no retry behavior;
- no distributed calls;
- no messaging;
- no caching;
- no resilience middleware;
- no service decomposition.

## Deferred mechanisms and authorizing evidence

- PostgreSQL: Cluster 1.2 consistency/transaction evidence
- semantic module boundaries: Cluster 1.3
- physical runtime split: Cluster 1.4 evidence
- retries/idempotency: later failure/duplicate evidence
- Kafka: demonstrated asynchronous durable communication need
- Redis: measured caching/state pressure
- Saga/Outbox: demonstrated multi-authority transaction/state-propagation pressure

## Next pressure

Part 1.1.2 must derive actors, goals, responsibilities and use cases from business reality without turning actors into services.
