package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderWorkflowSpringIntegrationTest {

    @Autowired
    PlaceOrderUseCase placeOrder;

    @Autowired
    OrderWorkflowUseCase workflow;

    @Autowired
    GetOrderUseCase getOrder;

    @Test
    void workflowMilestonesEvolveTheSameCurrentOrderThroughApplicationResults() {
        var placed = placeOrder.place(new PlaceOrderCommand(
                "customer-workflow",
                "restaurant-workflow",
                List.of(new PlaceOrderCommand.Line(
                        "item-1",
                        "Workflow Meal",
                        1,
                        new BigDecimal("12.00")
                ))
        ));

        assertThat(placed).isInstanceOf(PlaceOrderResult.Accepted.class);
        var orderId = ((PlaceOrderResult.Accepted) placed).order().id();
        var restaurantId = new RestaurantId("restaurant-workflow");

        assertAccepted(workflow.recordPayment(orderId));
        assertAccepted(workflow.recordRestaurantAcceptance(orderId, restaurantId));
        assertAccepted(workflow.recordPreparationStarted(orderId, restaurantId));
        assertAccepted(workflow.recordCompletion(orderId, restaurantId));

        var observed = getOrder.get(orderId);

        assertThat(observed.workflow())
                .extracting(OrderSnapshot.WorkflowOccurrence::action)
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        OrderWorkflowAction.PREPARATION_STARTED,
                        OrderWorkflowAction.ORDER_COMPLETED
                );

        assertThat(observed.status()).isEqualTo("COMPLETED");
    }

    private static void assertAccepted(OrderActionResult result) {
        assertThat(result).isInstanceOf(OrderActionResult.Accepted.class);
    }
}
