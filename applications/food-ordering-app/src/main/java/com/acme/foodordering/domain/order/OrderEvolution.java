package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;

import java.util.Objects;

/**
 * One logical Order evolution unit for the current in-process model.
 *
 * It deliberately binds the next lifecycle state to the workflow occurrence
 * that explains the evolution. This is not a database transaction and does not
 * claim durability, concurrency control, or distributed atomicity.
 */
record OrderEvolution(
        OrderStatus nextStatus,
        OrderWorkflowOccurrence occurrence
) {
    OrderEvolution {
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        Objects.requireNonNull(occurrence, "occurrence must not be null");
    }
}
