# ADR-0012 — Evidence Language and Claim Strength

Status: Accepted for C1.1-P12 candidate

## Context

C1.1-P01 through C1.1-P11 produced:
- business requirements;
- production code;
- architecture documentation;
- deterministic tests;
- CI executions;
- controlled failure experiments;
- inferred future mechanism needs;
- explicit non-guarantees.

The repository already uses phrases such as:
- executed and verified;
- structurally demonstrated;
- evidence-backed inference;
- not implemented;
- not guaranteed.

P12 makes those phrases precise.

The problem is not only documentation quality.

Incorrect evidence language creates architectural errors.

Examples:

"the test passed"
does not imply
"the system can never violate the property."

"the code contains a guard"
does not imply
"all callers always pass through the guard."

"ConcurrentHashMap is used"
does not imply
"the business read-modify-write operation is atomic."

"the current checkpoint has CI"
does not imply
"the system is production-ready."

## Decision

Use the following claim classes throughout Course 2.

### 1. Requirement

A normative statement about what should or must be true.

Examples:
- a cancelled Order must not enter preparation in the current lifecycle;
- completion state and completion history must remain coherent.

A requirement is not proved by existing code.

Tests may show that an implementation satisfied the requirement in selected scenarios.

### 2. Implementation statement

A factual statement about what the current repository contains or how the code is structured.

Examples:
- Order.transition checks the required source state;
- Order construction invokes OrderInvariants.verify;
- InMemoryOrderRepository stores current Orders in a ConcurrentHashMap.

Source inspection can structurally demonstrate an implementation statement.

Implementation presence does not by itself prove runtime behavior in every environment.

### 3. Observation

A statement about what happened in a specific execution under known conditions.

Examples:
- GitHub Actions run 37886764685 executed 68 tests with zero failures;
- the P11 status-first injected failure produced invariant-invalid partial state;
- the deterministic pre-replacement repository failure left the old PREPARING Order authoritative.

An observation is bounded by:
- exact code/configuration;
- exact test/harness;
- environment;
- workload;
- inputs;
- failure injection.

### 4. Evidence-backed inference

A conclusion that is not directly observed but is supported by observations plus mechanism reasoning.

Example:
- if future persistence stores lifecycle state and workflow history as independently committed records, an atomic commit boundary or equivalent correctness mechanism will be required if the P10 invariant must remain immediate.

The inference must state its assumptions.

### 5. Assumption

A premise currently accepted for reasoning but not established by the present evidence.

Examples:
- future PostgreSQL persistence may place state and history in separate rows/tables;
- a future external payment participant can fail independently;
- the current workload does not require distributed deployment.

Assumptions must never be silently rewritten as observations.

### 6. Scoped guarantee / non-guarantee

A guarantee is a claim that the implementation mechanism enforces within a clearly stated scope and under stated assumptions.

A guarantee must identify:
- property;
- scope/boundary;
- enforcing mechanism;
- assumptions;
- exclusions;
- supporting evidence.

Example of an acceptable scoped guarantee:

Within the current Order object construction path, assuming state changes occur through the current public domain methods and the private constructor is not bypassed, every successfully constructed Order is checked by OrderInvariants before it can be returned from that path.

This is much narrower than:

"The system guarantees consistency."

A non-guarantee is equally important.

Examples:
- no durability across process restart;
- no atomic find+evolve+save under competing callers;
- no PostgreSQL transaction guarantee;
- no distributed atomicity.

## Required wording discipline

Prefer:

"Observed in GitHub Actions run X under the current test harness."

"Structurally demonstrated by class/method Y."

"Evidence-backed inference, assuming Z."

"Scoped guarantee within boundary B, enforced by mechanism M."

"Not implemented / not guaranteed."

Avoid unsupported absolute wording such as:

"always"
"never fails"
"thread-safe business transaction"
"production-ready"
"highly available"
"scalable"
"exactly once"
"atomic"
"durable"

unless the scope and evidence justify it.

## Tests do not prove universality

A passing test establishes an observation for the exercised path.

Tests can increase confidence, falsify claims, and support a scoped guarantee when combined with mechanism reasoning.

Tests alone generally do not prove:
- all possible inputs;
- all thread interleavings;
- all crash points;
- all deployment configurations;
- all workloads;
- all dependency failures.

## Code inspection does not prove runtime behavior

Source may structurally demonstrate:
- a guard exists;
- an invariant checker is invoked;
- one repository implementation uses ConcurrentHashMap.

It does not by itself prove:
- Spring runtime wiring is correct;
- every adapter uses the path;
- no reflection/mapping bypass exists;
- behavior under concurrency/failure;
- production configuration.

Runtime evidence complements structural evidence.

## Evidence hierarchy is not a simple linear ranking

Different evidence answers different questions.

A source inspection may be stronger for:
"does this method contain a source-state guard?"

A deterministic failure test may be stronger for:
"what happens if save fails before replacement?"

A load test is required for:
"what throughput/latency occurs under workload W?"

A process-restart experiment is required for:
"does state survive restart?"

Choose evidence that matches the claim.

## Why P12 does not change production behavior

P12 is an evidence-discipline part.

The repository already has enough runtime behavior to audit.

Adding a new business mechanism would contaminate the distinction being learned and would be architecture change without a business/failure force.

The real project evolution for P12 is therefore:
- repository-wide claim vocabulary;
- claim-strength matrix;
- evidence audit;
- updated checkpoint/evidence manifest;
- full CI re-verification of the inherited application.

## Consequences

Positive:
- architecture claims become falsifiable;
- evidence scope is explicit;
- future parts can compare before/after mechanisms honestly;
- design reviews distinguish intention from proof;
- production incident reasoning is less likely to overgeneralize from one symptom.

Cost:
- documentation is more verbose;
- engineers must resist convenient absolute language;
- some conclusions remain deliberately weaker than desired until evidence is produced.

## Forward use

This vocabulary is mandatory for later:
- concurrency experiments;
- persistence/transaction claims;
- networking/timeouts;
- Kafka delivery semantics;
- caching;
- resilience;
- load/performance;
- availability/SLOs;
- replication/failover;
- distributed transactions.
