# C1.2-P06 — Value semantics experiment plan and interpretation

Inherited verified P05 2cc2277436b0729aed4e1712e7aa3cf9bad6521c; six targeted tests in OrderValueSemanticsContractTest.

E1 Record value equality: two equivalent OrderLine instances compare equal with equal hashCode and computed lineTotal.
E2 Monetary numeric versus structural equality: BigDecimal 5.0 and 5.00 compare numerically equal, but unequal via equals; OrderLine record equality differs, and lineTotal retains price scale.
E3 Invalid shapes: blank menuItemId/name, zero quantity, negative or null price rejected before Order lifecycle context.
E4 Defensive copying: caller mutates original list after placing Order; Order remains unchanged; getter list cannot be structurally modified.
E5 Replacing values: modifyLines constructs same-ID successor and appends ORDER_MODIFIED; earlier Order remains unchanged and replacement caller list is copied.
E6 Projection: OrderSnapshot.Line DTO is a detached value projection; snapshot total does not update with a later Order successor; lists unmodifiable.

Verification: exact P06 commit GitHub Actions must pass root Maven verify and inherited real P18 process-restart negative-control script. Note P18 passing reproduces known failure, not durable recovery. Avoid rewriting production code merely to create a checkpoint.
