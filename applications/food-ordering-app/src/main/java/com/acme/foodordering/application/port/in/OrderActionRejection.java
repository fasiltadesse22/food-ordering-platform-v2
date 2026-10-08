package com.acme.foodordering.application.port.in;

import java.util.Objects;

public record OrderActionRejection(
        Code code,
        String message
) {
    public OrderActionRejection {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public enum Code {
        ORDER_NOT_FOUND,
        ILLEGAL_TRANSITION,
        CUSTOMER_DOES_NOT_OWN_ORDER,
        RESTAURANT_DOES_NOT_OWN_ORDER,
        REFUND_REQUIRES_RECORDED_PAYMENT,
        REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER,
        REFUND_ALREADY_REQUESTED,
        MODIFICATION_REQUIRES_PLACED_ORDER
    }
}
