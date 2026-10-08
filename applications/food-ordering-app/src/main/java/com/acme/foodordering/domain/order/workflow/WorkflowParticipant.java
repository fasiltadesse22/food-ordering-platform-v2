package com.acme.foodordering.domain.order.workflow;

/**
 * Business participant labels used only to make P05 workflow handoffs explicit.
 *
 * These values are not authentication roles, service boundaries, or deployment
 * boundaries.
 */
public enum WorkflowParticipant {
    CUSTOMER,
    PAYMENT_PARTICIPANT,
    RESTAURANT_OPERATOR,
    PLATFORM
}
