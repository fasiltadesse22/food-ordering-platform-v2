package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.OrderId;

/**
 * COMMIT was attempted, but the repository did not receive confirmed success.
 * The transaction may have committed or rolled back. The caller must
 * reconcile the original Order/operation identity; blindly retrying a
 * business command can duplicate effects. This is NOT proof of rollback.
 */
public final class OrderCommitOutcomeUnknownException extends OrderPersistenceException {
    private final OrderId orderId;

    public OrderCommitOutcomeUnknownException(
            String operation, OrderId orderId, Throwable cause) {
        super(operation + " COMMIT outcome unconfirmed for Order " + orderId, cause);
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
