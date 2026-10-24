package com.acme.foodordering.application.port.in;

import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;

/** Typed classification boundary for a workflow action, independent of HTTP. */
public interface OrderWorkflowDecisionUseCase {
    OrderWorkflowResult execute(OrderId orderId, OrderWorkflowAction action);
}
