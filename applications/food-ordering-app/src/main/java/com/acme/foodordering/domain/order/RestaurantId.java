package com.acme.foodordering.domain.order;

public record RestaurantId(String value) {

    public RestaurantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("restaurant id must not be blank");
        }
    }
}
