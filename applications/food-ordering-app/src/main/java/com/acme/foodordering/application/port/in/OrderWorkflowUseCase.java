package com.acme.foodordering.application.port.in;

import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;

public interface OrderWorkflowUseCase {

    OrderActionResult recordPayment(OrderId orderId);

    OrderActionResult recordRestaurantAcceptance(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderActionResult recordRestaurantRejection(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderActionResult recordCancellation(
            OrderId orderId,
            CustomerId actingCustomerId
    );

    OrderActionResult recordRefundRequest(OrderId orderId);

    OrderActionResult recordPreparationStarted(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderActionResult recordCompletion(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );
}
