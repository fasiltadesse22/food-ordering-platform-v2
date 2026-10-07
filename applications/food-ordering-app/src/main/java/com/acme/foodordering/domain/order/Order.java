package com.acme.foodordering.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final List<OrderLine> lines;
    private final OrderStatus status;
    private final Instant placedAt;

    private Order(
            OrderId id,
            CustomerId customerId,
            RestaurantId restaurantId,
            List<OrderLine> lines,
            OrderStatus status,
            Instant placedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.restaurantId = Objects.requireNonNull(restaurantId);
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.placedAt = Objects.requireNonNull(placedAt);
    }

    public static Order place(
            OrderId id,
            CustomerId customerId,
            RestaurantId restaurantId,
            List<OrderLine> lines,
            Instant placedAt
    ) {
        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }

        return new Order(
                id,
                customerId,
                restaurantId,
                lines,
                OrderStatus.PLACED,
                placedAt
        );
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public RestaurantId restaurantId() {
        return restaurantId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public BigDecimal total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
