# Checkpoint Manifest — C1.1-P01 Candidate

## Identity

- Target checkpoint: `C1.1-P01`
- Current status: **CANDIDATE — NOT FROZEN**
- Inherited checkpoint: none
- Baseline: fresh V8.0 repository
- Candidate Git tag: `C1.1-P01-candidate`

## Engineering question

What must remain correct before we make any architectural decision?

## Prediction before implementation

A minimal real Food Ordering vertical slice can be implemented as one Spring Boot deployable with a framework-independent domain/application core and an in-memory output adapter, while deliberately avoiding premature distributed-system mechanisms.

## Source/config changes

See repository tree.

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

## Verification commands

Executed in the authoring environment:

```bash
java -version
./scripts/verify-core.sh
```

Required but **not executed successfully in the authoring environment**:

```bash
mvn -B -ntp verify
```

## Evidence status

- Java 21: observed
- framework-free core compile: executed and verified
- framework-free place/get behavior: executed and verified
- Maven reactor: not yet verified
- Spring context: not yet verified
- HTTP integration: not yet verified
- CI run: not yet verified

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

## Freeze gate

Do not relabel this candidate as `C1.1-P01` until:

1. `mvn -B -ntp verify` actually succeeds;
2. Spring context test executes;
3. HTTP integration tests execute;
4. the resulting output is recorded in the evidence ledger.
