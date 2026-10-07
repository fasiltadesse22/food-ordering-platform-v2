package com.acme.foodordering.application.service;

public final class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("order not found: " + orderId);
    }
}
