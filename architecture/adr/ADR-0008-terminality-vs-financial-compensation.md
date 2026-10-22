# ADR-0008 — Fulfillment closure is not financial reversal

**Status:** Accepted provisionally on independent P08 lineage, derived from V8.0 Part 1.1.8.

## Context
P07 enforces guarded Order transition pre/postconditions. A completed, rejected or cancelled fulfillment cannot simply be returned to an earlier primary status. The existing local workflow also permits recording a refund request after selected closed orders with recorded local payment. Confusing that request with refund completion would erase or fabricate financial facts.

## Decision
1. Preserve all P07 domain transition guards and postcondition enforcement unchanged.
2. Add read-only `OrderClosure`: primary fulfillment closure kind and a separate refund-request readiness classification derived from existing status and local accepted occurrence history.
3. Keep payment and refund-request occurrences in the original history. No `undoPayment`, `reopenTerminalOrder`, `setUnpaid` or automatic Saga mechanism.
4. Exercise refund-before-payment, duplicate request, cancel-after-completion and immutability of terminal Order collections in new tests.
5. Distinguish a permitted **local refund-request milestone** from real provider refund, reversal, cash settlement, financial compensation, and accounting outcomes.

## Alternatives
- **Reset state or delete payment occurrence:** rejected; falsifies accepted historical truth.
- **Treat refund request as a fulfillment state:** rejected; conflates separate lifecycles and can reopen a closed Order inadvertently.
- **Record an automatic refund on cancellation:** rejected; no provider integration or settled payment evidence yet.
- **Introduce Saga coordinator:** rejected as premature; distributed compensation design belongs to Phase IX once participants and failure semantics are established.
- **Add a generic Order editor solely for a terminal mutation test:** rejected; invents unsupported business functionality. We test the actual immutable collections and document the absence of edit API.

## Guarantees and limits
One-process sequential closure classification is deterministic from accepted local state and history. Once a legal local refund request is recorded, the earlier payment milestone stays present and fulfillment remains terminal. No external financial outcome, durability, concurrency serializability, actor authentication or global compensation is claimed. An external payment capture/refund would need its own authority, identity and reconciliation contract.

## Revisit when
Actual external provider transactions, cancellation/refund amount policies, new editing or correction commands, durable event history, or distributed compensation requirements are introduced.
