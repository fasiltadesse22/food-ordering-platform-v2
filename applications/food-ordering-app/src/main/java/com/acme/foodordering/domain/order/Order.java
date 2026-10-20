package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final List<OrderLine> lines;
    private final OrderStatus status;
    private final Instant placedAt;
    private final List<OrderWorkflowOccurrence> workflowOccurrences;

    private Order(
            OrderId id,
            CustomerId customerId,
            RestaurantId restaurantId,
            List<OrderLine> lines,
            OrderStatus status,
            Instant placedAt,
            List<OrderWorkflowOccurrence> workflowOccurrences
    ) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.restaurantId = Objects.requireNonNull(restaurantId);
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.placedAt = Objects.requireNonNull(placedAt);
        this.workflowOccurrences = List.copyOf(workflowOccurrences);
    }

    public static Order place(
            OrderId id,
            CustomerId customerId,
            RestaurantId restaurantId,
            List<OrderLine> lines,
            Instant placedAt
    ) {
        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }

        return new Order(
                id,
                customerId,
                restaurantId,
                lines,
                OrderStatus.PLACED,
                placedAt,
                List.of()
        );
    }

    /**
     * Records a payment-related local milestone under a provisional P06 guard.
     *
     * This is not an external provider call or settlement confirmation.
     */
    public Order recordPayment(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT,
                occurredAt
        );
    }

    public Order recordRestaurantAcceptance(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                occurredAt
        );
    }

    public Order recordRestaurantRejection(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.RESTAURANT_REJECTED,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                occurredAt
        );
    }

    public Order recordCancellation(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.ORDER_CANCELLED,
                WorkflowParticipant.CUSTOMER,
                occurredAt
        );
    }

    public Order recordRefundRequest(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.REFUND_REQUESTED,
                WorkflowParticipant.PLATFORM,
                occurredAt
        );
    }

    public Order recordPreparationStarted(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.PREPARATION_STARTED,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                occurredAt
        );
    }

    public Order recordCompletion(Instant occurredAt) {
        return transition(
                OrderWorkflowAction.ORDER_COMPLETED,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                occurredAt
        );
    }

    /**
     * Part 1.1.6: lifecycle legality lives in the domain, not the HTTP adapter.
     * A denied command creates neither a successor representation nor an
     * accepted workflow occurrence. Persistence is invoked only after success.
     * The read–decide–save sequence remains concurrency-naive deliberately.
     */
    private Order transition(
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        OrderStatus next = nextState(action);
        var updated = new ArrayList<>(workflowOccurrences);
        updated.add(new OrderWorkflowOccurrence(action, participant, occurredAt));
        return new Order(id, customerId, restaurantId, lines, next, placedAt, updated);
    }

    private OrderStatus nextState(OrderWorkflowAction action) {
        return switch (action) {
            case PAYMENT_RECORDED -> {
                // Money is a separate authority. This is only the local
                // payment-recorded milestone; never treat it as provider capture.
                if ((status != OrderStatus.PLACED && status != OrderStatus.ACCEPTED)
                        || has(OrderWorkflowAction.PAYMENT_RECORDED)) {
                    throw illegal(action, "payment is recordable once before preparation");
                }
                yield status;
            }
            case RESTAURANT_ACCEPTED -> {
                requireState(action, OrderStatus.PLACED);
                yield OrderStatus.ACCEPTED;
            }
            case RESTAURANT_REJECTED -> {
                requireState(action, OrderStatus.PLACED);
                yield OrderStatus.REJECTED;
            }
            case ORDER_CANCELLED -> {
                if (status != OrderStatus.PLACED && status != OrderStatus.ACCEPTED) {
                    throw illegal(action, "cancellation only while placed or accepted");
                }
                yield OrderStatus.CANCELLED;
            }
            case PREPARATION_STARTED -> {
                requireState(action, OrderStatus.ACCEPTED);
                if (!has(OrderWorkflowAction.PAYMENT_RECORDED)) {
                    throw illegal(action, "this provisional P06 policy requires recorded payment");
                }
                yield OrderStatus.PREPARING;
            }
            case ORDER_COMPLETED -> {
                requireState(action, OrderStatus.PREPARING);
                yield OrderStatus.COMPLETED;
            }
            case REFUND_REQUESTED -> {
                if (status != OrderStatus.CANCELLED && status != OrderStatus.REJECTED) {
                    throw illegal(action, "refund request follows a cancelled or rejected order");
                }
                if (!has(OrderWorkflowAction.PAYMENT_RECORDED)
                        || has(OrderWorkflowAction.REFUND_REQUESTED)) {
                    throw illegal(action, "requires prior recorded payment and no earlier request");
                }
                // A refund request does not reverse a terminal fulfillment
                // state or prove an external provider completed a refund.
                yield status;
            }
        };
    }

    private boolean has(OrderWorkflowAction action) {
        return workflowOccurrences.stream().anyMatch(x -> x.action() == action);
    }

    private void requireState(OrderWorkflowAction action, OrderStatus required) {
        if (status != required) {
            throw illegal(action, "requires source state " + required);
        }
    }

    private InvalidOrderTransitionException illegal(OrderWorkflowAction action, String reason) {
        return new InvalidOrderTransitionException(status, action, reason);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public RestaurantId restaurantId() {
        return restaurantId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public List<OrderWorkflowOccurrence> workflowOccurrences() {
        return workflowOccurrences;
    }

    public BigDecimal total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
