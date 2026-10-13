package com.acme.foodordering.application.port.out;
import com.acme.foodordering.domain.order.OrderId;
/** Existing storage row(s) cannot be reconstructed as a valid domain Order.
 * Not an ORDER_NOT_FOUND result; do not expose stored data in HTTP messages. */
public final class InvalidPersistedOrderException extends RuntimeException {
    public InvalidPersistedOrderException(OrderId id, Throwable cause) {
        super("Persisted Order is invalid: " + id,cause);
    }
}
