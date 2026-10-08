package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;

public enum OrderLifecycleTransition {

    ACCEPT(
            OrderStatus.PLACED,
            OrderStatus.ACCEPTED,
            OrderWorkflowAction.RESTAURANT_ACCEPTED,
            WorkflowParticipant.RESTAURANT_OPERATOR
    ),
    REJECT(
            OrderStatus.PLACED,
            OrderStatus.REJECTED,
            OrderWorkflowAction.RESTAURANT_REJECTED,
            WorkflowParticipant.RESTAURANT_OPERATOR
    ),
    CANCEL(
            OrderStatus.PLACED,
            OrderStatus.CANCELLED,
            OrderWorkflowAction.ORDER_CANCELLED,
            WorkflowParticipant.CUSTOMER
    ),
    START_PREPARATION(
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderWorkflowAction.PREPARATION_STARTED,
            WorkflowParticipant.RESTAURANT_OPERATOR
    ),
    COMPLETE(
            OrderStatus.PREPARING,
            OrderStatus.COMPLETED,
            OrderWorkflowAction.ORDER_COMPLETED,
            WorkflowParticipant.RESTAURANT_OPERATOR
    );

    private final OrderStatus source;
    private final OrderStatus target;
    private final OrderWorkflowAction workflowAction;
    private final WorkflowParticipant participant;

    OrderLifecycleTransition(
            OrderStatus source,
            OrderStatus target,
            OrderWorkflowAction workflowAction,
            WorkflowParticipant participant
    ) {
        this.source = source;
        this.target = target;
        this.workflowAction = workflowAction;
        this.participant = participant;
    }

    public OrderStatus source() {
        return source;
    }

    public OrderStatus target() {
        return target;
    }

    public OrderWorkflowAction workflowAction() {
        return workflowAction;
    }

    public WorkflowParticipant participant() {
        return participant;
    }
}
