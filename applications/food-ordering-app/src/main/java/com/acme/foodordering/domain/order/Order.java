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
     * Payment is intentionally modeled as an orthogonal workflow milestone in P06.
     * It does not change OrderStatus.
     */
    public Order recordPayment(Instant occurredAt) {
        return recordMilestone(
                OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT,
                occurredAt
        );
    }

    public Order recordRestaurantAcceptance(Instant occurredAt) {
        return transition(OrderLifecycleTransition.ACCEPT, occurredAt);
    }

    public Order recordRestaurantRejection(Instant occurredAt) {
        return transition(OrderLifecycleTransition.REJECT, occurredAt);
    }

    public Order recordCancellation(Instant occurredAt) {
        return transition(OrderLifecycleTransition.CANCEL, occurredAt);
    }

    /**
     * Refund request remains an orthogonal workflow milestone. P07/P08 will
     * deepen the conditions and compensation semantics.
     */
    public Order recordRefundRequest(Instant occurredAt) {
        return recordMilestone(
                OrderWorkflowAction.REFUND_REQUESTED,
                WorkflowParticipant.PLATFORM,
                occurredAt
        );
    }

    public Order recordPreparationStarted(Instant occurredAt) {
        return transition(OrderLifecycleTransition.START_PREPARATION, occurredAt);
    }

    public Order recordCompletion(Instant occurredAt) {
        return transition(OrderLifecycleTransition.COMPLETE, occurredAt);
    }

    private Order transition(
            OrderLifecycleTransition transition,
            Instant occurredAt
    ) {
        if (status != transition.source()) {
            throw new IllegalOrderTransitionException(id, status, transition);
        }

        return evolve(
                transition.target(),
                transition.workflowAction(),
                transition.participant(),
                occurredAt
        );
    }

    private Order recordMilestone(
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        return evolve(status, action, participant, occurredAt);
    }

    private Order evolve(
            OrderStatus nextStatus,
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        var updated = new ArrayList<>(workflowOccurrences);
        updated.add(new OrderWorkflowOccurrence(action, participant, occurredAt));

        return new Order(
                id,
                customerId,
                restaurantId,
                lines,
                nextStatus,
                placedAt,
                updated
        );
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
