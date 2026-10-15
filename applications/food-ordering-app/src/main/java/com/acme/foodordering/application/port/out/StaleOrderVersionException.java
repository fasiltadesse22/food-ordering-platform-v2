package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.OrderId;

/** Expected stored revision was superseded or the target Order no longer exists. */
public final class StaleOrderVersionException extends RuntimeException {
    private final OrderId orderId;
    private final long expectedVersion;
    public StaleOrderVersionException(OrderId id, long expectedVersion) {
        super("Order " + id + " is not at expected version " + expectedVersion);
        this.orderId=id;
        this.expectedVersion=expectedVersion;
    }
    public OrderId orderId(){ return orderId; }
    public long expectedVersion(){ return expectedVersion; }
}
