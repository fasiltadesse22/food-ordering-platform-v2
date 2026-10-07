package com.acme.foodordering.adapter.in.http;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
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
