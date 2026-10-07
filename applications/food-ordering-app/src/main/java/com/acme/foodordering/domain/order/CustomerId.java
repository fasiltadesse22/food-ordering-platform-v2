package com.acme.foodordering.domain.order;

public record CustomerId(String value) {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customer id must not be blank");
        }
    }
}
