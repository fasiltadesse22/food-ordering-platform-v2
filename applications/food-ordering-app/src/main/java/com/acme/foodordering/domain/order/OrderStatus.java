package com.acme.foodordering.domain.order;

/**
 * P06: primary fulfillment lifecycle. PAYMENT_RECORDED and REFUND_REQUESTED
 * remain orthogonal workflow occurrences, not independent Order statuses.
 * This is an explicitly provisional policy derived from the P05 scenarios.
 */
public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    COMPLETED,
    REJECTED,
    CANCELLED
}
