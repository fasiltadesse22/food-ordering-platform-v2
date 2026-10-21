package com.acme.foodordering.domain.order;

/**
 * P07: an accepted successor failed a programming contract; NOT a routine
 * business denial and NOT an external provider/network failure.
 */
public final class OrderTransitionContractViolationException extends IllegalStateException {
    public OrderTransitionContractViolationException(String reason) {
        super("Order transition postcondition violated: " + reason);
    }
}
