package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.OrderId;

/**
 * P05 application boundary for recording discovered workflow milestones.
 *
 * These operations intentionally do not define legal/illegal lifecycle
 * transitions. P06 will formalize that state-machine responsibility.
 */
public interface OrderWorkflowUseCase {

    OrderSnapshot recordPayment(OrderId orderId);

    OrderSnapshot recordRestaurantAcceptance(OrderId orderId);

    OrderSnapshot recordRestaurantRejection(OrderId orderId);

    OrderSnapshot recordCancellation(OrderId orderId);

    OrderSnapshot recordRefundRequest(OrderId orderId);

    OrderSnapshot recordPreparationStarted(OrderId orderId);

    OrderSnapshot recordCompletion(OrderId orderId);
}
