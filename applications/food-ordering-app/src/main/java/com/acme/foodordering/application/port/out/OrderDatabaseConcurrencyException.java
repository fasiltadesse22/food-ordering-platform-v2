package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.OrderId;
import java.sql.SQLException;
import java.util.Objects;

/**
 * PostgreSQL reported a recognized concurrency-related SQL failure before
 * COMMIT was attempted. Explicit classification is NOT automatic retry
 * permission. Unknown COMMIT acknowledgements retain their P15 exception.
 */
public final class OrderDatabaseConcurrencyException extends OrderPersistenceException {
    public enum Kind { SERIALIZATION_FAILURE, DEADLOCK_DETECTED, LOCK_NOT_AVAILABLE }
    private final OrderId orderId;
    private final Kind kind;
    private final String sqlState;
    public OrderDatabaseConcurrencyException(
            String operation, OrderId orderId, Kind kind, SQLException cause) {
        super(operation+" "+kind+" for Order "+orderId+" (SQLState "+cause.getSQLState()+")",cause);
        this.orderId=Objects.requireNonNull(orderId);
        this.kind=Objects.requireNonNull(kind);
        this.sqlState=Objects.requireNonNull(cause.getSQLState());
    }
    public OrderId orderId() { return orderId; }
    public Kind kind() { return kind; }
    public String sqlState() { return sqlState; }
}
