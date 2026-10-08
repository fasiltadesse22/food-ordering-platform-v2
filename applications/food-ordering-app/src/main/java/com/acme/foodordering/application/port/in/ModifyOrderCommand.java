package com.acme.foodordering.application.port.in;

import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.OrderId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ModifyOrderCommand(
        OrderId orderId,
        CustomerId actingCustomerId,
        List<Line> lines
) {
    public ModifyOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(actingCustomerId, "actingCustomerId must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
    }

    public record Line(
            String menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
