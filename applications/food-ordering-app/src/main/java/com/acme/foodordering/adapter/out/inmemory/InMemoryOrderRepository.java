package com.acme.foodordering.adapter.out.inmemory;

import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.port.out.OrderAlreadyExistsException;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Current in-process authority for Order.
 *
 * createNew inserts only when the identity is absent; saveCurrent replaces one
 * representation. That is useful local publication behavior, but it is NOT:
 * - durable persistence;
 * - a multi-object/database transaction;
 * - atomic read-modify-write across findCurrentById + saveCurrent;
 * - optimistic/pessimistic concurrency control;
 * - distributed atomicity.
 */
public final class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> currentOrders = new ConcurrentHashMap<>();

    /**
     * Atomic absent-key insertion for this single process-owned map.
     * A prior check-then-put would be unsafe under competing creates.
     */
    @Override
    public void createNew(Order order) {
        java.util.Objects.requireNonNull(order, "order must not be null");
        if (currentOrders.putIfAbsent(order.id(), order) != null) {
            throw new OrderAlreadyExistsException(order.id());
        }
    }

    @Override
    public void saveCurrent(Order order) {
        currentOrders.put(order.id(), order);
    }

    @Override
    public Optional<Order> findCurrentById(OrderId orderId) {
        return Optional.ofNullable(currentOrders.get(orderId));
    }

    public int size() {
        return currentOrders.size();
    }

    public void clear() {
        currentOrders.clear();
    }
}
