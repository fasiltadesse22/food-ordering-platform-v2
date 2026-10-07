package com.acme.foodordering.domain.order.fact;

import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A domain fact: in the current model, an order placement decision was accepted
 * and the resulting order became part of the application's current authoritative
 * in-process state.
 *
 * This type is not an integration message, broker record, or event-sourcing record.
 */
public record OrderPlaced(
        OrderId orderId,
        CustomerId customerId,
        RestaurantId restaurantId,
        Instant occurredAt,
        BigDecimal total
) {
    public OrderPlaced {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(restaurantId, "restaurantId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(total, "total must not be null");
    }

    public static OrderPlaced from(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        return new OrderPlaced(
                order.id(),
                order.customerId(),
                order.restaurantId(),
                order.placedAt(),
                order.total()
        );
    }
}
