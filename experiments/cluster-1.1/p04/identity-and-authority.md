# C1.1-P04 Experiment — Identity, Multiple Representations and Stale Observation

## Question

Can the application distinguish business identity from Java object identity and identify which same-identity representation is current?

## Hypothesis

If Order identity is value-based and repository authority is explicit:
- different OrderId objects with the same UUID value will address the same logical repository entry;
- different Order Java objects can represent the same Order identity;
- repository current state can change while an earlier OrderSnapshot remains unchanged;
- an unknown OrderId will not resolve to current state.

## Prediction

1. Two OrderId instances parsed from the same UUID are equal but not the same Java reference.
2. Saving an Order under the first ID allows lookup using the second equal ID.
3. Two distinct Order objects with the same OrderId occupy one repository identity slot; the later saveCurrent value is returned.
4. An earlier snapshot retains its original total after current repository state changes.
5. A well-formed unknown ID produces no repository state and GetOrderService reports not found.

## Setup

Executable test:

applications/food-ordering-app/src/test/java/com/acme/foodordering/application/semantics/OrderIdentityAndAuthorityTest.java

## Controlled variables

The experiments control:
- UUID identity value;
- Java object instance;
- quantity used to make two same-identity representations observably different;
- repository current mapping.

No lifecycle command is being modeled by the quantity change. It is a synthetic authority experiment only.

## Execution

GitHub Actions run: 37639145313

Command:

mvn -B -ntp verify

## Observation

OrderIdentityAndAuthorityTest:
- tests run: 4
- failures: 0
- errors: 0
- skipped: 0

Whole reactor:
- tests run: 15
- failures: 0
- errors: 0
- skipped: 0
- BUILD SUCCESS

Observed behavior matched the predictions.

## Evidence

- equal-valued OrderId objects were equal but not the same reference;
- lookup with an independently created equal OrderId resolved the stored Order;
- two different Order objects with equal OrderId values occupied one current repository identity slot;
- the second saveCurrent became the repository current representation;
- an earlier OrderSnapshot retained total 5.50 while repository current state for the same identity had total 11.00;
- unknown OrderId lookup returned empty and GetOrderService reported not found.

## Interpretation

Business identity is carried by OrderId value semantics, not Java object reference.

A detached snapshot is an observation. It can become stale while the current-state authority changes.

## Limitations

The experiment does not establish:
- legal order-modification semantics;
- concurrency safety;
- optimistic locking;
- persistence durability;
- database identity constraints;
- distributed authority.

## Conclusion

The controlled P04 execution supports the distinctions:

identity != object reference
snapshot != authority
observation != ownership
identifier != database primary key
