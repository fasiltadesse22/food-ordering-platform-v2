package com.acme.foodordering.domain.order;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OrderId random() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId from(String raw) {
        return new OrderId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
