package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.service.OrderSnapshot;
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
    void workflowMilestonesEvolveTheSameCurrentOrderThroughLegalLifecycleTransitions() {
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

        workflow.recordPayment(orderId);
        workflow.recordRestaurantAcceptance(orderId);
        workflow.recordPreparationStarted(orderId);
        workflow.recordCompletion(orderId);

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
}
