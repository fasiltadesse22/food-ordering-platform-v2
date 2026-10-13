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
     * Legacy unconditional replacement used for existing Order evolutions.
     * Deliberately NOT a stale-write-safe update: P13/P14/P17 negative-control
     * behavior must remain visible until P16–P18 real DB conflict evolution.
     * This method may currently insert absent identities. Do not use for
     * new-Order creation; use createNew instead.
     */
    void saveCurrent(Order order);

    Optional<Order> findCurrentById(OrderId orderId);
}
