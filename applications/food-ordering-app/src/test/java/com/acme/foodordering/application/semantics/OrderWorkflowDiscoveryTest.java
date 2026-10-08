package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.OrderId;
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

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-08T06:00:00Z"), ZoneOffset.UTC);
    private final PlaceOrderService placeOrder = new PlaceOrderService(repository, clock);
    private final OrderWorkflowService workflow = new OrderWorkflowService(repository, clock);

    @Test
    void happyPathRecordsPaymentAcceptancePreparationAndCompletionHandoffs() {
        var orderId = placeOrder();

        workflow.recordPayment(orderId);
        workflow.recordRestaurantAcceptance(orderId);
        workflow.recordPreparationStarted(orderId);
        var completed = workflow.recordCompletion(orderId);

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

        // P05 records workflow but deliberately does not yet formalize lifecycle state.
        assertThat(completed.status()).isEqualTo("PLACED");
    }

    @Test
    void restaurantRejectionAfterRecordedPaymentExposesRefundHandoff() {
        var orderId = placeOrder();

        workflow.recordPayment(orderId);
        workflow.recordRestaurantRejection(orderId);
        var refundRequested = workflow.recordRefundRequest(orderId);

        assertThat(actions(refundRequested)).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_REJECTED,
                OrderWorkflowAction.REFUND_REQUESTED
        );

        assertThat(participants(refundRequested)).containsExactly(
                WorkflowParticipant.PAYMENT_PARTICIPANT,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                WorkflowParticipant.PLATFORM
        );
    }

    @Test
    void cancellationBeforePaymentIsASeparateWorkflowBranch() {
        var orderId = placeOrder();

        var cancelled = workflow.recordCancellation(orderId);

        assertThat(actions(cancelled)).containsExactly(
                OrderWorkflowAction.ORDER_CANCELLED
        );
        assertThat(participants(cancelled)).containsExactly(
                WorkflowParticipant.CUSTOMER
        );
    }

    @Test
    void cancellationAfterRecordedPaymentCanExposeRefundRequirement() {
        var orderId = placeOrder();

        workflow.recordPayment(orderId);
        workflow.recordCancellation(orderId);
        var refundRequested = workflow.recordRefundRequest(orderId);

        assertThat(actions(refundRequested)).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.ORDER_CANCELLED,
                OrderWorkflowAction.REFUND_REQUESTED
        );
    }

    @Test
    void preStateMachineRecorderStillAllowsContradictoryAndOutOfOrderMilestones() {
        var orderId = placeOrder();

        workflow.recordPreparationStarted(orderId);
        workflow.recordRestaurantAcceptance(orderId);
        workflow.recordRestaurantRejection(orderId);
        var completed = workflow.recordCompletion(orderId);

        assertThat(actions(completed)).containsExactly(
                OrderWorkflowAction.PREPARATION_STARTED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.RESTAURANT_REJECTED,
                OrderWorkflowAction.ORDER_COMPLETED
        );

        // This PASS is evidence of a missing state-machine guard, not a business guarantee.
        assertThat(completed.status()).isEqualTo("PLACED");
    }

    private OrderId placeOrder() {
        var result = placeOrder.place(new PlaceOrderCommand(
                "customer-1",
                "restaurant-1",
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
