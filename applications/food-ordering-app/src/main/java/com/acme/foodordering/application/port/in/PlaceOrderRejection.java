package com.acme.foodordering.application.port.in;

import java.util.Objects;

public record PlaceOrderRejection(
        Code code,
        String message
) {
    public PlaceOrderRejection {
        Objects.requireNonNull(code, "code must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("rejection message must not be blank");
        }
    }

    public enum Code {
        ACTOR_TYPE_NOT_ALLOWED,
        ACTOR_CUSTOMER_MISMATCH
    }
}
