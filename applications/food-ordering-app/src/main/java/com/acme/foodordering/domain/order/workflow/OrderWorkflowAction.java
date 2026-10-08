package com.acme.foodordering.domain.order.workflow;

/**
 * Business-workflow occurrences associated with the current learning model.
 *
 * These are not Kafka messages and this list is not an Event Store.
 */
public enum OrderWorkflowAction {
    PAYMENT_RECORDED,
    RESTAURANT_ACCEPTED,
    RESTAURANT_REJECTED,
    ORDER_CANCELLED,
    REFUND_REQUESTED,
    ORDER_MODIFIED,
    PREPARATION_STARTED,
    ORDER_COMPLETED
}
