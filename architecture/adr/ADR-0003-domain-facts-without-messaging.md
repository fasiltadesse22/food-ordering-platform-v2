# ADR-0003 — Represent Domain Facts Without Introducing Messaging

Status: Accepted for C1.1-P03

## Context

Part 1.1.3 must distinguish actor intent, application commands, decisions, accepted/rejected outcomes, and domain facts.

A common failure mode is to introduce Kafka, an event bus, an outbox, or integration-event schemas merely because the word event appears in domain discussion.

No evidence currently requires asynchronous transport, durable publication, replay, independent consumers, or cross-process propagation.

## Decision

Represent the accepted Place Order fact as the immutable domain type OrderPlaced.

Return it only on the accepted application result.

Do not publish it to a broker, persist it as an event stream, or claim that it is an integration event.

## Why

The semantic question is:

Did the requested intent become an accepted business fact?

That question can be answered inside one process.

Messaging would introduce unrelated concerns: serialization contracts, broker availability, producer acknowledgements, retries, duplicates, ordering, consumer lag, outbox/dual-write problems, and schema evolution.

## Consequences

Positive:
- command and fact semantics become executable;
- rejected commands cannot be described as completed facts;
- later messaging decisions retain a clean pre-broker baseline.

Negative:
- no fact publication or replay exists;
- no durable event history exists;
- other processes cannot observe OrderPlaced.

These are intentional non-guarantees.

## Reversal / evolution criteria

A later part may introduce integration messaging only when there is evidence for asynchronous communication, independent consumers, durable propagation, or another explicit distributed-system force.

At that point the domain fact may be mapped into a separate integration message. The mapping must not erase the distinction between the internal business fact and its transport representation.
