package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import java.util.List;
import java.util.Objects;

/**
 * P10: invariants over an accepted Order's *whole* local lifecycle.
 * Replays accepted occurrences to show the claimed final status is consistent
 * with the entire legal causal path. No durable/event-sourced audit implied.
 *
 * This is deliberately distinct from P07's operation-local admission guard:
 * it can reject a corrupt accepted snapshot even when all individual fields
 * satisfy null/type/enum/database-style constraints.
 */
final class OrderBusinessInvariants {
    private OrderBusinessInvariants() {}

    static void requireValid(OrderStatus claimed, List<OrderWorkflowOccurrence> occurrences) {
        Objects.requireNonNull(claimed, "claimed status");
        Objects.requireNonNull(occurrences, "occurrence list");
        OrderStatus state = OrderStatus.PLACED;
        boolean payment = false;
        boolean refund = false;
        for (int i = 0; i < occurrences.size(); i++) {
            var occurrence = Objects.requireNonNull(occurrences.get(i), "occurrence");
            var action = occurrence.action();
            WorkflowParticipant expectedActor = switch (action) {
                case PAYMENT_RECORDED -> WorkflowParticipant.PAYMENT_PARTICIPANT;
                case RESTAURANT_ACCEPTED, RESTAURANT_REJECTED,
                     PREPARATION_STARTED, ORDER_COMPLETED -> WorkflowParticipant.RESTAURANT_OPERATOR;
                case ORDER_CANCELLED -> WorkflowParticipant.CUSTOMER;
                case REFUND_REQUESTED -> WorkflowParticipant.PLATFORM;
            };
            require(occurrence.participant() == expectedActor,
                    "wrong modeled participant at occurrence " + i);
            switch (action) {
                case PAYMENT_RECORDED -> {
                    require((state == OrderStatus.PLACED || state == OrderStatus.ACCEPTED) && !payment,
                            "payment may be recorded once before preparation");
                    payment = true;
                }
                case RESTAURANT_ACCEPTED -> {
                    require(state == OrderStatus.PLACED, "acceptance requires placed Order");
                    state = OrderStatus.ACCEPTED;
                }
                case RESTAURANT_REJECTED -> {
                    require(state == OrderStatus.PLACED, "rejection requires placed Order");
                    state = OrderStatus.REJECTED;
                }
                case ORDER_CANCELLED -> {
                    require(state == OrderStatus.PLACED || state == OrderStatus.ACCEPTED,
                            "cannot cancel after preparation or terminal fulfillment");
                    state = OrderStatus.CANCELLED;
                }
                case PREPARATION_STARTED -> {
                    require(state == OrderStatus.ACCEPTED && payment,
                            "preparation requires acceptance and prior local payment");
                    state = OrderStatus.PREPARING;
                }
                case ORDER_COMPLETED -> {
                    require(state == OrderStatus.PREPARING, "completion requires preparation");
                    state = OrderStatus.COMPLETED;
                }
                case REFUND_REQUESTED -> {
                    require((state == OrderStatus.CANCELLED || state == OrderStatus.REJECTED)
                            && payment && !refund,
                            "refund request requires closed fulfillment, prior local payment and no prior request");
                    refund = true;
                }
            }
        }
        require(state == claimed,
                "claimed status " + claimed + " differs from status implied by accepted history " + state);
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new OrderInvariantViolationException(reason);
    }
}
