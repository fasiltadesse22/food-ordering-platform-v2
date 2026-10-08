package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;

/**
 * Application boundary for the selected Order workflow.
 *
 * P07 makes business context explicit for lifecycle actions. The supplied
 * customer/restaurant identity is a claimed business context, not proof that
 * authentication has occurred.
 */
public interface OrderWorkflowUseCase {

    OrderSnapshot recordPayment(OrderId orderId);

    OrderSnapshot recordRestaurantAcceptance(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderSnapshot recordRestaurantRejection(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderSnapshot recordCancellation(
            OrderId orderId,
            CustomerId actingCustomerId
    );

    OrderSnapshot recordRefundRequest(OrderId orderId);

    OrderSnapshot recordPreparationStarted(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );

    OrderSnapshot recordCompletion(
            OrderId orderId,
            RestaurantId actingRestaurantId
    );
}
