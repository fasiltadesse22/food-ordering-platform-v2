# ADR-0025 — Preserve Validated OrderLine Value Semantics; Defer Monetary Normalization

Status: Accepted for C1.2-P06, revisitable under explicit business requirements.
Base: checkpoints/C1.2-P05 (2cc2277436b0729aed4e1712e7aa3cf9bad6521c).

## Decision
Keep current validated OrderLine record, its component-wise equality and BigDecimal price representation. Keep the Order's List.copyOf defensive copying and derived total. Add 6 deterministic tests exposing equality, price-scale, invalid construction, aliasing, immutable evolution and DTO detachment. Do not silently normalize BigDecimal scale or introduce Money/currency, JPA mapping, line-entity IDs, taxes or rounding policy.

## Rationale
Current value object records selected line facts; two separately created identical values are substitutable in the present model. BigDecimal scale is part of Java equals behavior; no contract chooses price-scale normalization, supported currency, or a canonical numeric equality regime. Silent change could change hash-based collections and persistence roundtrip semantics. Derived total avoids a second mutable monetary authority.

## Tradeoffs
Value equality is not always business equality; name snapshots, menu IDs, customization or temporal price contracts may matter later.
List.copyOf prevents collection aliasing but is not deep copy for hypothetical mutable elements.
Record syntax is neither sufficient evidence of correct domain modeling nor equivalent to an API DTO.
No current concurrency, durability, payment correctness or historical audit completeness guarantee follows from immutable values.

## Revisit triggers
Pricing/currency requirements, discount/tax policy, gateway agreement amounts, DB NUMERIC scale mapping, explicit canonicalization/equality rules, line-level identity and lifecycle, product-catalog relationship or measured update cost.
