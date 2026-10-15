package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.port.out.StaleOrderVersionException;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.IllegalOrderTransitionException;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderGuardViolationException;
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
    public OrderActionResult recordPayment(OrderId orderId) {
        return update(orderId, current -> current.recordPayment(clock.instant()), ConflictPolicy.NO_RETRY);
    }

    @Override
    public OrderActionResult recordRestaurantAcceptance(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordRestaurantAcceptance(
                        actingRestaurantId,
                        clock.instant()
                ), ConflictPolicy.RE_EVALUATE_ONCE
        );
    }

    @Override
    public OrderActionResult recordRestaurantRejection(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordRestaurantRejection(
                        actingRestaurantId,
                        clock.instant()
                ), ConflictPolicy.RE_EVALUATE_ONCE
        );
    }

    @Override
    public OrderActionResult recordCancellation(
            OrderId orderId,
            CustomerId actingCustomerId
    ) {
        return update(
                orderId,
                current -> current.recordCancellation(
                        actingCustomerId,
                        clock.instant()
                ), ConflictPolicy.RE_EVALUATE_ONCE
        );
    }

    @Override
    public OrderActionResult recordRefundRequest(OrderId orderId) {
        return update(orderId, current -> current.recordRefundRequest(clock.instant()), ConflictPolicy.NO_RETRY);
    }

    @Override
    public OrderActionResult recordPreparationStarted(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordPreparationStarted(
                        actingRestaurantId,
                        clock.instant()
                ), ConflictPolicy.RE_EVALUATE_ONCE
        );
    }

    @Override
    public OrderActionResult recordCompletion(
            OrderId orderId,
            RestaurantId actingRestaurantId
    ) {
        return update(
                orderId,
                current -> current.recordCompletion(
                        actingRestaurantId,
                        clock.instant()
                ), ConflictPolicy.RE_EVALUATE_ONCE
        );
    }

    /**
     * P18: only selected pure-local lifecycle decisions may be re-evaluated
     * once after a confirmed stale-version rejection. The retry begins with
     * a NEW repository read and a NEW domain decision; it NEVER resubmits
     * the stale immutable successor. Payment/refund local milestones are
     * deliberately NO_RETRY: duplicate effects and operation identity are
     * not yet solved. No retry on SQLState failures or unknown COMMIT outcome.
     */
    private enum ConflictPolicy { NO_RETRY, RE_EVALUATE_ONCE }

    private OrderActionResult update(
            OrderId orderId,
            Function<Order, Order> evolution,
            ConflictPolicy policy
    ) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(evolution, "evolution must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        int maxAttempts = policy == ConflictPolicy.RE_EVALUATE_ONCE ? 2 : 1;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Do not reuse an earlier Order snapshot after a conflict.
            var current = repository.findCurrentById(orderId);
            if (current.isEmpty()) {
                return new OrderActionResult.Rejected(new OrderActionRejection(
                        OrderActionRejection.Code.ORDER_NOT_FOUND,
                        "order " + orderId + " was not found"
                ));
            }
            try {
                var updated = evolution.apply(current.get());
                repository.saveCurrent(updated);
                return new OrderActionResult.Accepted(OrderSnapshot.from(updated));
            } catch (StaleOrderVersionException conflict) {
                if (attempt + 1 < maxAttempts) {
                    continue;
                }
                return new OrderActionResult.Rejected(new OrderActionRejection(
                        OrderActionRejection.Code.STALE_ORDER_VERSION,
                        conflict.getMessage()
                ));
            } catch (IllegalOrderTransitionException exception) {
                return new OrderActionResult.Rejected(new OrderActionRejection(
                        OrderActionRejection.Code.ILLEGAL_TRANSITION,
                        exception.getMessage()
                ));
            } catch (OrderGuardViolationException exception) {
                return new OrderActionResult.Rejected(new OrderActionRejection(
                        mapGuardCode(exception.code()),
                        exception.getMessage()
                ));
            }
        }
        throw new IllegalStateException("unreachable: bounded attempt loop");
    }

    private static OrderActionRejection.Code mapGuardCode(
            OrderGuardViolationException.Code code
    ) {
        return switch (code) {
            case CUSTOMER_DOES_NOT_OWN_ORDER ->
                    OrderActionRejection.Code.CUSTOMER_DOES_NOT_OWN_ORDER;
            case RESTAURANT_DOES_NOT_OWN_ORDER ->
                    OrderActionRejection.Code.RESTAURANT_DOES_NOT_OWN_ORDER;
            case REFUND_REQUIRES_RECORDED_PAYMENT ->
                    OrderActionRejection.Code.REFUND_REQUIRES_RECORDED_PAYMENT;
            case REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER ->
                    OrderActionRejection.Code.REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER;
            case REFUND_ALREADY_REQUESTED ->
                    OrderActionRejection.Code.REFUND_ALREADY_REQUESTED;
            case MODIFICATION_REQUIRES_PLACED_ORDER ->
                    OrderActionRejection.Code.MODIFICATION_REQUIRES_PLACED_ORDER;
        };
    }
}
