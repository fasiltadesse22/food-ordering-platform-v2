package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.time.Clock;
import java.util.Objects;
import java.util.function.BiFunction;

public final class OrderWorkflowService implements OrderWorkflowUseCase {

    private final OrderRepository repository;
    private final Clock clock;

    public OrderWorkflowService(OrderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OrderSnapshot recordPayment(OrderId orderId) {
        return update(orderId, Order::recordPayment);
    }

    @Override
    public OrderSnapshot recordRestaurantAcceptance(OrderId orderId) {
        return update(orderId, Order::recordRestaurantAcceptance);
    }

    @Override
    public OrderSnapshot recordRestaurantRejection(OrderId orderId) {
        return update(orderId, Order::recordRestaurantRejection);
    }

    @Override
    public OrderSnapshot recordCancellation(OrderId orderId) {
        return update(orderId, Order::recordCancellation);
    }

    @Override
    public OrderSnapshot recordRefundRequest(OrderId orderId) {
        return update(orderId, Order::recordRefundRequest);
    }

    @Override
    public OrderSnapshot recordPreparationStarted(OrderId orderId) {
        return update(orderId, Order::recordPreparationStarted);
    }

    @Override
    public OrderSnapshot recordCompletion(OrderId orderId) {
        return update(orderId, Order::recordCompletion);
    }

    private OrderSnapshot update(
            OrderId orderId,
            BiFunction<Order, java.time.Instant, Order> recorder
    ) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        var current = repository.findCurrentById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        var updated = recorder.apply(current, clock.instant());

        repository.saveCurrent(updated);
        return OrderSnapshot.from(updated);
    }
}
