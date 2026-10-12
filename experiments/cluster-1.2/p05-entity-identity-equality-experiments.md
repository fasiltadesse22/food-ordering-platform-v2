# C1.2-P05 — Entity identity/equality experiments

Base: checkpoints/C1.2-P04, 0c3efa497887930618e6ab9dc3b7503b09c5ceb8.
New tests: OrderEntityIdentityEqualityTest (5 targeted cases).

E1: create PLACED then ACCEPTED: separate references and false Order.equals; equal business IDs; original retains PLACED.
E2: two Orders with equal business contents but differing IDs: independent repository entries.
E3: HashSet<Order> retains two same-ID immutable instances; HashSet<OrderId> retains one equal business identifier.
E4: typed CustomerId and RestaurantId remain non-equal even if underlying text matches; OrderId value equality survives parsing.
E5: terminal COMPLETED lifecycle preserves the original OrderId and earlier snapshots remain unchanged.

Expected: these tests pass at root Maven verify without production changes.
Inherited negative controls: C1.1-P17 (same-ID stale replacements remain possible), C1.1-P18 (process restart still loses Order).
Evidence must distinguish domain identity continuity, Java equals/hashCode and authority. CI run/commit exact SHA is the verification source.
