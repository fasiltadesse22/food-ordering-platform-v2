package com.acme.foodordering.domain.order;

public final class OrderGuardViolationException extends RuntimeException {

    public enum Code {
        CUSTOMER_DOES_NOT_OWN_ORDER,
        RESTAURANT_DOES_NOT_OWN_ORDER,
        REFUND_REQUIRES_RECORDED_PAYMENT,
        REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER,
        REFUND_ALREADY_REQUESTED,
        MODIFICATION_REQUIRES_PLACED_ORDER
    }

    private final OrderId orderId;
    private final Code code;

    public OrderGuardViolationException(OrderId orderId, Code code, String message) {
        super(message);
        this.orderId = orderId;
        this.code = code;
    }

    public OrderId orderId() {
        return orderId;
    }

    public Code code() {
        return code;
    }
}
