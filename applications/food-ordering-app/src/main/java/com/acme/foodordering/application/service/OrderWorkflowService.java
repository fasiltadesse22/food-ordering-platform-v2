package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Function;

public final class OrderWorkflowService implements OrderWorkflowUseCase {

    private final OrderRepository repository;
    private final Clock clock;

    public OrderWorkflowService(OrderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OrderSnapshot recordPayment(OrderId orderId) {
        return update(orderId, current -> current.recordPayment(clock.instant()));
    }

    @Override
    public OrderSnapshot recordRestaurantAcceptance(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordRestaurantAcceptance(
                        actingRestaurantId,
                        clock.instant()
                )
        );
    }

    @Override
    public OrderSnapshot recordRestaurantRejection(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordRestaurantRejection(
                        actingRestaurantId,
                        clock.instant()
                )
        );
    }

    @Override
    public OrderSnapshot recordCancellation(
            OrderId orderId,
            CustomerId actingCustomerId
    ) {
        return update(
                orderId,
                current -> current.recordCancellation(
                        actingCustomerId,
                        clock.instant()
                )
        );
    }

    @Override
    public OrderSnapshot recordRefundRequest(OrderId orderId) {
        return update(orderId, current -> current.recordRefundRequest(clock.instant()));
    }

    @Override
    public OrderSnapshot recordPreparationStarted(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordPreparationStarted(
                        actingRestaurantId,
                        clock.instant()
                )
        );
    }

    @Override
    public OrderSnapshot recordCompletion(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordCompletion(
                        actingRestaurantId,
                        clock.instant()
                )
        );
    }

    private OrderSnapshot update(
            OrderId orderId,
            Function<Order, Order> evolution
    ) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(evolution, "evolution must not be null");

        var current = repository.findCurrentById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        var updated = evolution.apply(current);

        repository.saveCurrent(updated);
        return OrderSnapshot.from(updated);
    }
}
