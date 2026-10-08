package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;

import java.util.List;
import java.util.Objects;

final class OrderInvariants {

    private OrderInvariants() {}

    static void verify(
            OrderStatus status,
            List<OrderWorkflowOccurrence> workflowOccurrences
    ) {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(
                workflowOccurrences,
                "workflowOccurrences must not be null"
        );

        var cancelled = has(
                workflowOccurrences,
                OrderWorkflowAction.ORDER_CANCELLED
        );
        var rejected = has(
                workflowOccurrences,
                OrderWorkflowAction.RESTAURANT_REJECTED
        );
        var accepted = has(
                workflowOccurrences,
                OrderWorkflowAction.RESTAURANT_ACCEPTED
        );
        var preparationStarted = has(
                workflowOccurrences,
                OrderWorkflowAction.PREPARATION_STARTED
        );
        var completed = has(
                workflowOccurrences,
                OrderWorkflowAction.ORDER_COMPLETED
        );

        if (cancelled && status != OrderStatus.CANCELLED) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .CANCELLED_FACT_REQUIRES_CANCELLED_STATE,
                    "ORDER_CANCELLED history requires CANCELLED current state, but was "
                            + status
            );
        }

        if (rejected && status != OrderStatus.REJECTED) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .REJECTED_FACT_REQUIRES_REJECTED_STATE,
                    "RESTAURANT_REJECTED history requires REJECTED current state, but was "
                            + status
            );
        }

        if (completed && status != OrderStatus.COMPLETED) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .COMPLETION_FACT_REQUIRES_COMPLETED_STATE,
                    "ORDER_COMPLETED history requires COMPLETED current state, but was "
                            + status
            );
        }

        if (preparationStarted
                && status != OrderStatus.PREPARING
                && status != OrderStatus.COMPLETED) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .PREPARATION_FACT_REQUIRES_PREPARING_OR_COMPLETED_STATE,
                    "PREPARATION_STARTED history requires PREPARING or COMPLETED current state, but was "
                            + status
            );
        }

        if (status == OrderStatus.ACCEPTED && !accepted) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .ACCEPTED_STATE_REQUIRES_ACCEPTANCE_FACT,
                    "ACCEPTED state requires RESTAURANT_ACCEPTED history"
            );
        }

        if (status == OrderStatus.PREPARING
                && (!accepted || !preparationStarted)) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .PREPARING_STATE_REQUIRES_ACCEPTANCE_AND_PREPARATION_FACTS,
                    "PREPARING state requires RESTAURANT_ACCEPTED and PREPARATION_STARTED history"
            );
        }

        if (status == OrderStatus.COMPLETED
                && (!accepted || !preparationStarted || !completed)) {
            throw violation(
                    OrderInvariantViolationException.Code
                            .COMPLETED_STATE_REQUIRES_ACCEPTANCE_PREPARATION_AND_COMPLETION_FACTS,
                    "COMPLETED state requires acceptance, preparation and completion history"
            );
        }
    }

    private static boolean has(
            List<OrderWorkflowOccurrence> workflowOccurrences,
            OrderWorkflowAction action
    ) {
        return workflowOccurrences.stream()
                .anyMatch(occurrence -> occurrence.action() == action);
    }

    private static OrderInvariantViolationException violation(
            OrderInvariantViolationException.Code code,
            String message
    ) {
        return new OrderInvariantViolationException(code, message);
    }
}
