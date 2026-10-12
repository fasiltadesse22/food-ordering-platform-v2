package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.OrderId;

import java.util.Objects;

/**
 * Repository identity conflict: an attempted *new* Order already exists.
 *
 * Not a domain lifecycle rejection, HTTP 409 mapping, or proof of repeated
 * logical customer intent. New creation must not erase an existing identity.
 */
public final class OrderAlreadyExistsException extends RuntimeException {
    private final OrderId orderId;

    public OrderAlreadyExistsException(OrderId orderId) {
        super("Order identity already exists: " + Objects.requireNonNull(orderId));
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
