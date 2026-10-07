package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.PlaceOrderRejection;

public record PlaceOrderRejectedResponse(
        String code,
        String message
) {
    static PlaceOrderRejectedResponse from(PlaceOrderRejection rejection) {
        return new PlaceOrderRejectedResponse(
                rejection.code().name(),
                rejection.message()
        );
    }
}
