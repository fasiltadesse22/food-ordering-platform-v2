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

Authoritative command:

mvn -B -ntp verify

## Observation

Pending P04 CI.

## Limitations

The experiment does not establish:
- legal order modification semantics;
- concurrency safety;
- optimistic locking;
- persistence durability;
- database identity constraints;
- distributed authority.

## Conclusion

Pending execution evidence.
