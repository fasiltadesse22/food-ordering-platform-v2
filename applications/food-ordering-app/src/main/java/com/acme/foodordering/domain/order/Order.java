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

    public Order recordPayment(Instant occurredAt) {
        return recordMilestone(
                OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT,
                occurredAt
        );
    }

    public Order recordRestaurantAcceptance(
            RestaurantId actingRestaurantId,
            Instant occurredAt
    ) {
        return transition(
                OrderLifecycleTransition.ACCEPT,
                occurredAt,
                () -> requireOwningRestaurant(actingRestaurantId)
        );
    }

    public Order recordRestaurantRejection(
            RestaurantId actingRestaurantId,
            Instant occurredAt
    ) {
        return transition(
                OrderLifecycleTransition.REJECT,
                occurredAt,
                () -> requireOwningRestaurant(actingRestaurantId)
        );
    }

    public Order recordCancellation(
            CustomerId actingCustomerId,
            Instant occurredAt
    ) {
        return transition(
                OrderLifecycleTransition.CANCEL,
                occurredAt,
                () -> requireOwningCustomer(actingCustomerId)
        );
    }

    /**
     * REFUND_REQUESTED is a compensating workflow action. It does not erase
     * PAYMENT_RECORDED and does not reopen/change the terminal Order lifecycle.
     */
    public Order recordRefundRequest(Instant occurredAt) {
        requireRefundEligible();
        requireRefundNotAlreadyRequested();

        return recordMilestone(
                OrderWorkflowAction.REFUND_REQUESTED,
                WorkflowParticipant.PLATFORM,
                occurredAt
        );
    }

    public Order modifyLines(
            CustomerId actingCustomerId,
            List<OrderLine> replacementLines,
            Instant occurredAt
    ) {
        requireOwningCustomer(actingCustomerId);
        requirePlacedForModification();

        Objects.requireNonNull(replacementLines, "replacementLines must not be null");
        if (replacementLines.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        var updatedWorkflow = new ArrayList<>(workflowOccurrences);
        updatedWorkflow.add(new OrderWorkflowOccurrence(
                OrderWorkflowAction.ORDER_MODIFIED,
                WorkflowParticipant.CUSTOMER,
                occurredAt
        ));

        return new Order(
                id,
                customerId,
                restaurantId,
                replacementLines,
                status,
                placedAt,
                updatedWorkflow
        );
    }

    public Order recordPreparationStarted(
            RestaurantId actingRestaurantId,
            Instant occurredAt
    ) {
        return transition(
                OrderLifecycleTransition.START_PREPARATION,
                occurredAt,
                () -> requireOwningRestaurant(actingRestaurantId)
        );
    }

    public Order recordCompletion(
            RestaurantId actingRestaurantId,
            Instant occurredAt
    ) {
        return transition(
                OrderLifecycleTransition.COMPLETE,
                occurredAt,
                () -> requireOwningRestaurant(actingRestaurantId)
        );
    }

    private Order transition(
            OrderLifecycleTransition transition,
            Instant occurredAt,
            Runnable contextualGuard
    ) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(contextualGuard, "contextualGuard must not be null");

        requireLegalSourceState(transition);
        contextualGuard.run();

        return evolve(
                transition.target(),
                transition.workflowAction(),
                transition.participant(),
                occurredAt
        );
    }

    private void requireLegalSourceState(OrderLifecycleTransition transition) {
        if (status != transition.source()) {
            throw new IllegalOrderTransitionException(id, status, transition);
        }
    }

    private void requireOwningCustomer(CustomerId actingCustomerId) {
        Objects.requireNonNull(actingCustomerId, "actingCustomerId must not be null");

        if (!customerId.equals(actingCustomerId)) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.CUSTOMER_DOES_NOT_OWN_ORDER,
                    "customer " + actingCustomerId.value()
                            + " cannot act on order " + id
                            + " owned by customer " + customerId.value()
            );
        }
    }

    private void requireOwningRestaurant(RestaurantId actingRestaurantId) {
        Objects.requireNonNull(actingRestaurantId, "actingRestaurantId must not be null");

        if (!restaurantId.equals(actingRestaurantId)) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.RESTAURANT_DOES_NOT_OWN_ORDER,
                    "restaurant " + actingRestaurantId.value()
                            + " cannot act on order " + id
                            + " assigned to restaurant " + restaurantId.value()
            );
        }
    }

    private void requireRefundEligible() {
        if (status != OrderStatus.REJECTED && status != OrderStatus.CANCELLED) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER,
                    "refund request requires order " + id
                            + " to be REJECTED or CANCELLED, but was " + status
            );
        }

        if (!hasWorkflowAction(OrderWorkflowAction.PAYMENT_RECORDED)) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.REFUND_REQUIRES_RECORDED_PAYMENT,
                    "refund request requires a recorded payment for order " + id
            );
        }
    }

    private void requireRefundNotAlreadyRequested() {
        if (hasWorkflowAction(OrderWorkflowAction.REFUND_REQUESTED)) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.REFUND_ALREADY_REQUESTED,
                    "refund has already been requested for order " + id
            );
        }
    }

    private void requirePlacedForModification() {
        if (status != OrderStatus.PLACED) {
            throw new OrderGuardViolationException(
                    id,
                    OrderGuardViolationException.Code.MODIFICATION_REQUIRES_PLACED_ORDER,
                    "order modification requires PLACED state, but order "
                            + id + " was " + status
            );
        }
    }

    private boolean hasWorkflowAction(OrderWorkflowAction action) {
        return workflowOccurrences.stream()
                .anyMatch(occurrence -> occurrence.action() == action);
    }

    private Order recordMilestone(
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
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
