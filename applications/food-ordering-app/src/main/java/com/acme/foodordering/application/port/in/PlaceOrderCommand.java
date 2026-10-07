package com.acme.foodordering.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record PlaceOrderCommand(
        ActorContext actor,
        String customerId,
        String restaurantId,
        List<Line> lines
) {
    public PlaceOrderCommand {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
    }

    public PlaceOrderCommand(
            String customerId,
            String restaurantId,
            List<Line> lines
    ) {
        this(ActorContext.customer(customerId), customerId, restaurantId, lines);
    }

    public record Line(
            String menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
