package com.acme.foodordering.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderCommand(
        String customerId,
        String restaurantId,
        List<Line> lines
) {
    public record Line(
            String menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
