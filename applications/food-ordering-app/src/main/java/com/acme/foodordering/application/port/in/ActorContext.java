package com.acme.foodordering.application.port.in;

import java.util.Objects;

public record ActorContext(
        String actorId,
        ActorType actorType
) {
    public ActorContext {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actor id must not be blank");
        }
        Objects.requireNonNull(actorType, "actor type must not be null");
    }

    public static ActorContext customer(String customerId) {
        return new ActorContext(customerId, ActorType.CUSTOMER);
    }

    public static ActorContext restaurantOperator(String operatorId) {
        return new ActorContext(operatorId, ActorType.RESTAURANT_OPERATOR);
    }
}
