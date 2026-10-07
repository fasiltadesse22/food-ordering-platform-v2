package com.acme.foodordering.application.service;

import com.acme.foodordering.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSnapshot(
        String id,
        String customerId,
        String restaurantId,
        String status,
        Instant placedAt,
        BigDecimal total,
        List<Line> lines
) {
    public record Line(
            String menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public static OrderSnapshot from(Order order) {
        return new OrderSnapshot(
                order.id().toString(),
                order.customerId().value(),
                order.restaurantId().value(),
                order.status().name(),
                order.placedAt(),
                order.total(),
                order.lines().stream()
                        .map(line -> new Line(
                                line.menuItemId(),
                                line.name(),
                                line.quantity(),
                                line.unitPrice(),
                                line.lineTotal()
                        ))
                        .toList()
        );
    }
}
