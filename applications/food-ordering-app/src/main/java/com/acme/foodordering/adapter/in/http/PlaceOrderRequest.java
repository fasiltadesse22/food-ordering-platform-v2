package com.acme.foodordering.adapter.in.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
        @NotBlank String customerId,
        @NotBlank String restaurantId,
        @NotNull @Size(min = 1) @Valid List<Line> lines
) {
    public record Line(
            @NotBlank String menuItemId,
            @NotBlank String name,
            @Positive int quantity,
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice
    ) {}
}
