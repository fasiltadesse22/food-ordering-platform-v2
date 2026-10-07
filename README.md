# Food Ordering Platform — V8.0 Fresh-Start Baseline

This repository is the evolving project for **Enterprise Distributed Systems Architecture & System Design — V8.0**.

Current learning checkpoint:

```text
C1.1-P01 candidate
```

It intentionally starts with **one real Spring Boot deployable** and does not pre-create microservices or future infrastructure.

## Requirements

- Java 21
- Maven 3.9+
- Spring Boot 4.1.1 (managed by the root POM)

## Full verification gate

```bash
mvn -B -ntp verify
```

## Framework-free fallback verification

```bash
./scripts/verify-core.sh
```

The fallback verifies only the JDK-only core slice and does **not** replace the V8 Maven checkpoint gate.

## Run

After Maven dependencies are available:

```bash
mvn -pl applications/food-ordering-app spring-boot:run
```

## Example

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{
        "customerId":"customer-1",
        "restaurantId":"restaurant-1",
        "lines":[
          {
            "menuItemId":"burger-1",
            "name":"Classic Burger",
            "quantity":2,
            "unitPrice":5.50
          }
        ]
      }' \
  http://localhost:8080/orders
```

## Architectural guardrails

Not present by design:

- PostgreSQL
- Kafka
- Redis
- Saga
- Outbox
- CQRS
- Event Sourcing
- distributed locks
- service mesh
- Kubernetes
- database-per-service
- premature service decomposition

See `checkpoint-manifest.md` and `architecture/evidence/C1.1-P01-evidence.md` for exact verification status.
