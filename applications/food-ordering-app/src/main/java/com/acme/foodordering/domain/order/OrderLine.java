package com.acme.foodordering.domain.order;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderLine(
        String menuItemId,
        String name,
        int quantity,
        BigDecimal unitPrice
) {
    public OrderLine {
        if (menuItemId == null || menuItemId.isBlank()) {
            throw new IllegalArgumentException("menu item id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("menu item name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unit price must not be null");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unit price must not be negative");
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
