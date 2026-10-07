package com.acme.foodordering.adapter.out.inmemory;

import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
