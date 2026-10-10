# ADR-0014 — Separate Cancellation/Acceptance Business Policy From Concurrency Mechanism

Status: Accepted for C1.1-P14 candidate

## Context

P13 proved the generic mechanism:

find current Order
→ decide against captured snapshot
→ unconditional save

allows two mutually exclusive lifecycle decisions to both return Accepted when they derive from the same PLACED Order.

P14 applies that mechanism to the roadmap-required race between:
- Customer CANCEL;
- Restaurant ACCEPT.

This conflict is more significant than P13 ACCEPT/REJECT because the competing operations belong to different actors with different goals, knowledge and possible downstream consequences.

## Current domain facts

From PLACED:

CANCEL is individually legal:
PLACED → CANCELLED

ACCEPT is individually legal:
PLACED → ACCEPTED

Sequentially:
- cancel first means later accept is illegal;
- accept first means later cancel is illegal under the current lifecycle.

Therefore the two outcomes are mutually exclusive for one PLACED Order version.

## Minimal correctness requirement

The current lifecycle cannot legitimately acknowledge both cancellation and acceptance as successful decisions from one logical PLACED version if they cannot coexist in any valid serial history.

At most one may become the authoritative lifecycle decision.

## Important unresolved question

P14 does NOT decide which actor has priority when both intents overlap.

Concurrency control answers:

"How do we ensure one coherent authoritative outcome?"

Business conflict policy answers:

"Which outcome is allowed to win under which business conditions?"

These are different questions.

## Candidate business policies

### Policy A — First authoritative commit wins

Whichever decision successfully commits against the still-current PLACED version wins.

The loser detects state/version change and is reevaluated/rejected.

Advantages:
- simple;
- maps naturally to optimistic conflict detection;
- no wall-clock ordering policy required.

Trade-off:
- infrastructure scheduling can influence business outcome.

### Policy B — Customer cancellation priority before a defined cutoff

If cancellation intent is registered before a business-defined commitment boundary, cancellation wins.

Potential boundary examples would require later business clarification, such as:
- before restaurant acceptance is committed;
- before preparation starts;
- before a contractual cutoff.

Advantages:
- customer-friendly semantics.

Trade-offs:
- requires an authoritative definition of "registered before";
- time/order semantics become part of correctness;
- may require stronger coordination.

### Policy C — Restaurant acceptance priority after fulfillment commitment

Once a defined acceptance/fulfillment commitment has become authoritative, cancellation becomes a later cancellation/compensation workflow rather than a PLACED→CANCELLED transition.

Advantages:
- protects restaurant operational commitment.

Trade-offs:
- customer cancellation semantics become more complex;
- refund/compensation policies may be needed.

## Rejected accidental policy

Current unconditional last-write-wins is NOT accepted as business policy.

Reason:
- whichever save happens last wins due to implementation timing;
- both actors can receive success;
- the earlier acknowledged decision can disappear;
- no business rationale selects the winner.

## Decision for P14

Preserve the race.

Do not add locking/versioning.

Do not select Policy A/B/C without business evidence.

Record:
- both serial orderings;
- both shared-snapshot save orderings;
- participant knowledge;
- current accidental last-write-wins behavior;
- policy alternatives and their forces.

## Why no timestamp winner

A naive "earliest timestamp wins" rule is not adopted.

It would require answers about:
- whose clock;
- request creation time versus receipt time versus commit time;
- clock skew;
- retries;
- network delay;
- authoritative sequencing.

P14 has no evidence to authorize that complexity.

## Forward evolution

A later persistence/concurrency mechanism may detect one decision as stale.

That mechanism still must map the conflict into the chosen business policy.

P15 studies repeated commands, not this race policy.
P17 deepens stale-decision reasoning.
Cluster 1.2 will provide real persistence/versioning pressure.
