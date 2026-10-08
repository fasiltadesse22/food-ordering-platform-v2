package com.acme.foodordering.adapter.in.http;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank String customerId
) {}
