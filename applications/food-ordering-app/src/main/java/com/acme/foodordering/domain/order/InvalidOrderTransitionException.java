package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;

import java.util.Objects;

/** Expected business denial: current state does not permit the requested action. */
public final class InvalidOrderTransitionException extends IllegalStateException {
    private final OrderStatus source;
    private final OrderWorkflowAction action;

    public InvalidOrderTransitionException(OrderStatus source, OrderWorkflowAction action, String reason) {
        super("Cannot apply " + action + " in " + source + ": " + reason);
        this.source = Objects.requireNonNull(source);
        this.action = Objects.requireNonNull(action);
    }

    public OrderStatus source() { return source; }
    public OrderWorkflowAction action() { return action; }
}
