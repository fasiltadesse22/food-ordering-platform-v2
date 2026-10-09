package com.acme.foodordering.adapter.out.inmemory;

import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Current in-process authority for Order.
 *
 * saveCurrent replaces one map entry with one complete immutable Order
 * representation. That is useful local publication behavior, but it is NOT:
 * - durable persistence;
 * - a multi-object/database transaction;
 * - atomic read-modify-write across findCurrentById + saveCurrent;
 * - optimistic/pessimistic concurrency control;
 * - distributed atomicity.
 */
public final class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> currentOrders = new ConcurrentHashMap<>();

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
