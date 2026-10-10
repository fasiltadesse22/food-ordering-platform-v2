# ADR-0020 — Derive an Order-Centered Consistency-Boundary Candidate From Invariants

Status: PROVISIONAL, analysis-only (C1.2-P01; not a final aggregate or persistence commitment)
Inherited: `checkpoints/C1.1-FINAL` at `d76cfdf585e19afeef21b9d522351541c5cbc07f`.

## Context
C1.1 proved selected Order construction/status/history invariants, actor and lifecycle guards, and one locally valid immutable successor.
It also proved (a) split-status/history intermediate invalidity in P11, (b) accepted contradictory concurrent decisions in P13/P14,
(c) stale successor overwrites and state resurrection in P17, and (d) loss of acknowledged Order state after JVM replacement in P18.
The current repository has no durable storage, versioned saves, SQL transactions or external charge authority.

## Decision
Treat the current Order identity and its immediate lifecycle representation
(status, relevant workflow occurrences, ownership identifiers, and lines as needed for the specific operation)
as a **provisional Order-centered consistency-boundary candidate**.
Preserve the final C1.1 code and failure experiments unchanged while tracing all rule-to-state dependencies.
Do not conflate logical boundary, Java class, table, transaction, bounded context or deployable service.

## Rationale
- Selected invariants explicitly connect status and selected occurrences.
- Current lifecycle decisions require Order state and actor identity/ownership checks.
- Direct domain evolution can create coherent successors with simple implementation.
- Separate independent authority for status and required history would impose avoidable partial-state coordination.
- Owning whole Customer/Restaurant/provider models is not necessary to preserve established local Order invariants.

## Alternatives and consequences
A. Keep status/history and other immediate Order rules locally governed together — preferred **candidate**; preserves simplicity; may create contention if future workload or histories are large.
B. Separate status/history consistency authorities now — rejected provisionally: creates failure windows contradicting P11 requirements.
C. Enlarge boundary to own Restaurant/Customer/payment-provider state — rejected provisionally: would blur external authority and increase coupling.
D. Declare one aggregate per future service/database — rejected: aggregate != bounded context != service != database.
E. Use distributed locks/Saga/Event Sourcing for P01 — rejected: mechanism pressure and conceptual prerequisites not established.

## Guarantees and non-guarantees
Structurally demonstrated: current Order constructors verify selected invariants; one immutable successor groups selected status/history changes.
Observed in inherited executed tests: P11 split failure, P13 conflicts, P17 stale corruption, P18 restart loss.
NOT guaranteed: safe concurrent read-modify-write, version freshness, restart durability, external payment integrity, fully valid history, or immediate multi-authority consistency.

## Consequences, risks and follow-up
- P01 is Type A: documents and decision notes only, no runtime code/config/schema changes.
- Preserve actual C1.1-FINAL branch as before-state.
- P04 must test the proposed aggregate boundary, P10 repository semantics, P11–P15 durability/atomic transactions, P16–P18 concurrency controls, P19–P21 contention/cross-aggregate boundaries.
- Open policy decisions remain open; this ADR is neither a topology ADR nor a final payment workflow decision.

## Revisit triggers
Changed business invariant, separate ownership with independent lifecycle, significant hot-aggregate contention,
large history write amplification, new operational release pressure, or observed coordination failure.
