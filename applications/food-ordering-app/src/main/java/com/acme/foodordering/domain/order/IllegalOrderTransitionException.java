package com.acme.foodordering.domain.order;

public final class IllegalOrderTransitionException extends RuntimeException {

    private final OrderId orderId;
    private final OrderStatus currentStatus;
    private final OrderLifecycleTransition attemptedTransition;

    public IllegalOrderTransitionException(
            OrderId orderId,
            OrderStatus currentStatus,
            OrderLifecycleTransition attemptedTransition
    ) {
        super("order " + orderId
                + " cannot perform " + attemptedTransition
                + " while in " + currentStatus);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedTransition = attemptedTransition;
    }

    public OrderId orderId() {
        return orderId;
    }

    public OrderStatus currentStatus() {
        return currentStatus;
    }

    public OrderLifecycleTransition attemptedTransition() {
        return attemptedTransition;
    }
}
