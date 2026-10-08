package com.acme.foodordering.domain.order.workflow;

/**
 * Discovered business-workflow milestones after Order placement.
 *
 * P05 deliberately does not define legal/illegal state transitions. These
 * milestones are a workflow trace, not a state machine.
 */
public enum OrderWorkflowAction {
    PAYMENT_RECORDED,
    RESTAURANT_ACCEPTED,
    RESTAURANT_REJECTED,
    ORDER_CANCELLED,
    REFUND_REQUESTED,
    PREPARATION_STARTED,
    ORDER_COMPLETED
}
