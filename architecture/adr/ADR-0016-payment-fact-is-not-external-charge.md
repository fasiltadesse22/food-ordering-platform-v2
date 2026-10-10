# ADR-0016 — PAYMENT_RECORDED Is Not an External Charge Guarantee

Status: Accepted for C1.1-P16 candidate

## Context

Since P05, PAYMENT_RECORDED has intentionally represented a local workflow milestone.

It has never been a real payment-provider integration or a durable financial ledger.

P15 demonstrated that repeated local commands require replay semantics.

P16 asks what changes when the repeated command can trigger an independently committed external effect such as charging money.

The current Order.recordPayment method:
- has no duplicate guard;
- appends PAYMENT_RECORDED;
- does not represent a provider transaction ID;
- does not establish external payment authority.

## Decision

Keep production behavior unchanged.

Use a controlled test-only payment authority to model an independently committed external effect.

The test authority is deliberately not presented as a production payment provider.

P16 will establish four facts:
1. repeated local recordPayment can append duplicate PAYMENT_RECORDED occurrences;
2. charge-then-record can duplicate both external charges and local facts on repeated execution;
3. an external charge may commit while its response is lost, leaving caller/application knowledge uncertain;
4. record-then-charge merely reverses the inconsistency window and can leave local PAYMENT_RECORDED without an external charge.

## Required distinction

PAYMENT_RECORDED
!=
payment-provider charge
!=
payment-provider acknowledgement
!=
money movement guarantee.

The local Order repository is authority for the current in-process Order representation.

The modeled external payment authority is separately authoritative for its committed charge count.

Neither current mechanism atomically owns both authorities.

## Why no production payment port yet

P16 is proving the force, not selecting the final payment architecture.

Adding a real provider adapter, durable payment entity, idempotency store, Saga, Outbox, Kafka, or distributed transaction would collapse later roadmap learning.

The current test-only boundary is sufficient to demonstrate the correctness problem without claiming a production integration exists.

## Why call ordering is insufficient

### Charge then record

Failure window:
external charge commits
→ response lost / process fails
→ PAYMENT_RECORDED not written.

Retry may charge again.

### Record then charge

Failure window:
PAYMENT_RECORDED written
→ provider fails before charge.

Local history can claim payment was recorded even though external effect did not happen.

Therefore:
ordering the two non-atomic steps does not create atomicity.

## Forward consequences

P17 will study stale-state decisions.

P18 will study process crash/transient-state/durability non-guarantees.

Later dedicated idempotency work will introduce stable operation identity and replay handling.

Later payment/distributed workflow evolution may require compensation/reconciliation rather than pretending one local transaction can atomically own remote money movement.
