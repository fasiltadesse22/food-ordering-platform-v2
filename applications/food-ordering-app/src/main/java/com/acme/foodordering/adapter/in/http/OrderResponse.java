package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.service.OrderSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
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

    public static OrderResponse from(OrderSnapshot snapshot) {
        return new OrderResponse(
                snapshot.id(),
                snapshot.customerId(),
                snapshot.restaurantId(),
                snapshot.status(),
                snapshot.placedAt(),
                snapshot.total(),
                snapshot.lines().stream()
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
