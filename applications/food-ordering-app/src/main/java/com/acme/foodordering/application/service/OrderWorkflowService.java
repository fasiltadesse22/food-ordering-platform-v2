package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowDecisionUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowResult;
import com.acme.foodordering.domain.order.InvalidOrderTransitionException;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.time.Clock;
import java.util.Objects;
import java.util.function.BiFunction;

public final class OrderWorkflowService implements OrderWorkflowUseCase, OrderWorkflowDecisionUseCase {

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

    /**
     * P09 expected denial is a business result; unknown Order, contract
     * breach, and infrastructure failures are not converted to rejections.
     */
    @Override
    public OrderWorkflowResult execute(OrderId id, OrderWorkflowAction action) {
        Objects.requireNonNull(action, "action");
        try {
            OrderSnapshot accepted = switch (action) {
                case PAYMENT_RECORDED -> recordPayment(id);
                case RESTAURANT_ACCEPTED -> recordRestaurantAcceptance(id);
                case RESTAURANT_REJECTED -> recordRestaurantRejection(id);
                case ORDER_CANCELLED -> recordCancellation(id);
                case REFUND_REQUESTED -> recordRefundRequest(id);
                case PREPARATION_STARTED -> recordPreparationStarted(id);
                case ORDER_COMPLETED -> recordCompletion(id);
            };
            return new OrderWorkflowResult.Accepted(accepted);
        } catch (InvalidOrderTransitionException denied) {
            return new OrderWorkflowResult.Rejected(
                    new OrderWorkflowResult.BusinessRejection(
                        OrderWorkflowResult.BusinessRejection.Code.ILLEGAL_LIFECYCLE_TRANSITION,
                        action, denied.getMessage()
                    )
            );
        }
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
