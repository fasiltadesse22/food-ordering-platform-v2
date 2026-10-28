package com.acme.foodordering.domain.order;

/**
 * Internal corruption/implementation defect, NOT a policy denial or bad HTTP input.
 * No client should be told its attempted transition was illegal merely because
 * an accepted aggregate state failed to satisfy its invariant.
 */
public final class OrderInvariantViolationException extends IllegalStateException {
    public OrderInvariantViolationException(String reason) {
        super("Order business invariant violated: " + reason);
    }
}
