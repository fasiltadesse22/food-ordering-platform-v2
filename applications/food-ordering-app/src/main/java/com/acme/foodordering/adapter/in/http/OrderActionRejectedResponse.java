package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.OrderActionRejection;

public record OrderActionRejectedResponse(
        String code,
        String message
) {
    static OrderActionRejectedResponse from(OrderActionRejection rejection) {
        return new OrderActionRejectedResponse(
                rejection.code().name(),
                rejection.message()
        );
    }
}
