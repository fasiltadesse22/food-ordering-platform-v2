package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWorkflowDiscoveryTest {

    private static final CustomerId CUSTOMER = new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-1");

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-08T06:00:00Z"),
            ZoneOffset.UTC
    );
    private final PlaceOrderService placeOrder = new PlaceOrderService(repository, clock);
    private final OrderWorkflowService workflow = new OrderWorkflowService(repository, clock);

    @Test
    void happyPathPreservesWorkflowAndLifecycleState() {
        var orderId = placeOrder();

        var paid = accepted(workflow.recordPayment(orderId));
        var accepted = accepted(workflow.recordRestaurantAcceptance(orderId, RESTAURANT));
        var preparing = accepted(workflow.recordPreparationStarted(orderId, RESTAURANT));
        var completed = accepted(workflow.recordCompletion(orderId, RESTAURANT));

        assertThat(paid.status()).isEqualTo("PLACED");
        assertThat(accepted.status()).isEqualTo("ACCEPTED");
        assertThat(preparing.status()).isEqualTo("PREPARING");
        assertThat(completed.status()).isEqualTo("COMPLETED");

        assertThat(actions(completed)).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.PREPARATION_STARTED,
                OrderWorkflowAction.ORDER_COMPLETED
        );

        assertThat(participants(completed)).containsExactly(
                WorkflowParticipant.PAYMENT_PARTICIPANT,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                WorkflowParticipant.RESTAURANT_OPERATOR
        );
    }

    @Test
    void restaurantRejectionAfterPaymentAllowsRefundRequest() {
        var orderId = placeOrder();

        accepted(workflow.recordPayment(orderId));
        var rejected = accepted(workflow.recordRestaurantRejection(orderId, RESTAURANT));
        var refundRequested = accepted(workflow.recordRefundRequest(orderId));

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(refundRequested.status()).isEqualTo("REJECTED");

        assertThat(actions(refundRequested)).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_REJECTED,
                OrderWorkflowAction.REFUND_REQUESTED
        );
    }

    @Test
    void cancellationFromPlacedStateProducesCancelledLifecycleState() {
        var orderId = placeOrder();

        var cancelled = accepted(workflow.recordCancellation(orderId, CUSTOMER));

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(actions(cancelled)).containsExactly(
                OrderWorkflowAction.ORDER_CANCELLED
        );
    }

    @Test
    void cancellationAfterPaymentAllowsRefundRequest() {
        var orderId = placeOrder();

        var paid = accepted(workflow.recordPayment(orderId));
        var cancelled = accepted(workflow.recordCancellation(orderId, CUSTOMER));
        var refundRequested = accepted(workflow.recordRefundRequest(orderId));

        assertThat(paid.status()).isEqualTo("PLACED");
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(refundRequested.status()).isEqualTo("CANCELLED");

        assertThat(actions(refundRequested)).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.ORDER_CANCELLED,
                OrderWorkflowAction.REFUND_REQUESTED
        );
    }

    @Test
    void preparationBeforeAcceptanceIsBusinessRejectionAtApplicationBoundary() {
        var orderId = placeOrder();

        var result = workflow.recordPreparationStarted(orderId, RESTAURANT);

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        var rejection = ((OrderActionResult.Rejected) result).rejection();
        assertThat(rejection.code())
                .isEqualTo(OrderActionRejection.Code.ILLEGAL_TRANSITION);

        var current = repository.findCurrentById(orderId).orElseThrow();
        assertThat(current.status().name()).isEqualTo("PLACED");
        assertThat(current.workflowOccurrences()).isEmpty();
    }

    private OrderId placeOrder() {
        var result = placeOrder.place(new PlaceOrderCommand(
                CUSTOMER.value(),
                RESTAURANT.value(),
                List.of(new PlaceOrderCommand.Line(
                        "burger-1",
                        "Classic Burger",
                        2,
                        new BigDecimal("5.50")
                ))
        ));

        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        return ((PlaceOrderResult.Accepted) result).order().id();
    }

    private static OrderSnapshot accepted(OrderActionResult result) {
        assertThat(result).isInstanceOf(OrderActionResult.Accepted.class);
        return ((OrderActionResult.Accepted) result).order();
    }

    private static List<OrderWorkflowAction> actions(OrderSnapshot snapshot) {
        return snapshot.workflow().stream()
                .map(OrderSnapshot.WorkflowOccurrence::action)
                .toList();
    }

    private static List<WorkflowParticipant> participants(OrderSnapshot snapshot) {
        return snapshot.workflow().stream()
                .map(OrderSnapshot.WorkflowOccurrence::participant)
                .toList();
    }
}
