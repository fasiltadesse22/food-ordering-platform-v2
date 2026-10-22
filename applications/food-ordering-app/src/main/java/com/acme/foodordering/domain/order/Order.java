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
     * P07 contract order:
     * 1. classify business preconditions before an accepted mutation,
     * 2. construct exactly one immutable successor only on success,
     * 3. verify its postconditions before the application can save it.
     * Sequential, process-local correctness only; read–decide–save still races.
     */
    private Order transition(
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(participant, "participant must not be null");
        OrderStatus next = OrderTransitionContract.requirePreconditions(this, action);

        var updated = new ArrayList<>(workflowOccurrences);
        updated.add(new OrderWorkflowOccurrence(action, participant, occurredAt));
        Order successor = new Order(id, customerId, restaurantId, lines, next, placedAt, updated);

        OrderTransitionContract.requirePostconditions(
                this, successor, action, participant, occurredAt, next
        );
        return successor;
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

    /**
     * Part 1.1.8: distinguish terminal fulfillment decisions from separate
     * financial follow-up milestones. This is an interpretation of local
     * history, not an authoritative provider refund/capture status.
     */
    public OrderClosure closure() {
        return OrderClosure.from(this);
    }

    public BigDecimal total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
