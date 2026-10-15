package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Optional;

/**
 * Current-state authority for Order within the application boundary.
 *
 * The selected backend defines authority scope: in-memory is process-local,
 * while PostgreSQL supports durable committed state. This interface alone
 * is not a durability or stale-write guarantee and it is not authority
 * for restaurant, payment, customer-account, or other external truths.
 */
public interface OrderRepository {

    /**
     * Publish a previously absent aggregate identity. If that identity already
     * exists, throw OrderAlreadyExistsException without changing the current
     * authoritative representation. Implementations must decide existence and
     * publication as one atomic operation in their own authority scope.
     *
     * This is NOT request-level idempotency, database durability, nor a
     * cross-process uniqueness guarantee for the in-memory implementation.
     */
    void createNew(Order order);

    /**
     * Conditionally publish an existing successor. Domain evolution advances
     * version by exactly one; this method succeeds only when the stored root
     * still has successor.version()-1. Otherwise it throws
     * StaleOrderVersionException without changing any aggregate state.
     * This is NOT command-level idempotency or automatic retry handling.
     */
    void saveCurrent(Order order);

    Optional<Order> findCurrentById(OrderId orderId);
}
