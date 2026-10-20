package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLifecycleStateMachineTest {
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private Order placed() {
        return Order.place(
                OrderId.random(), new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine("burger-1", "Burger", 1, new BigDecimal("5.50"))),
                NOW);
    }

    private Order paid(Order source) { return source.recordPayment(NOW); }
    private Order accepted(Order source) { return source.recordRestaurantAcceptance(NOW); }
    private Order preparing(Order source) { return source.recordPreparationStarted(NOW); }
    private Order completed() { return preparing(accepted(paid(placed()))).recordCompletion(NOW); }
    private Order cancelled() { return placed().recordCancellation(NOW); }
    private Order rejected() { return placed().recordRestaurantRejection(NOW); }

    @Test
    void initialStateIsPlacedAndHasNoPostPlacementFacts() {
        var o = placed();
        assertThat(o.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(o.workflowOccurrences()).isEmpty();
    }

    @Test
    void legalHappyPathPreservesIdentityAndEstablishesCompletedOnlyAfterPreparation() {
        var initial = placed();
        var recorded = paid(initial);
        var accepted = accepted(recorded);
        var preparing = preparing(accepted);
        var completed = preparing.recordCompletion(NOW);

        assertThat(recorded.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(preparing.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.id()).isEqualTo(initial.id());
        assertThat(completed.workflowOccurrences())
                .extracting(x -> x.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        OrderWorkflowAction.PREPARATION_STARTED,
                        OrderWorkflowAction.ORDER_COMPLETED);
        assertThat(initial.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(initial.workflowOccurrences()).isEmpty();
    }

    @Test
    void paymentCanBeRecordedAfterAcceptanceButBeforePreparation() {
        var accepted = accepted(placed());
        var paidAfterAcceptance = paid(accepted);
        assertThat(paidAfterAcceptance.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(preparing(paidAfterAcceptance).status()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void restaurantRejectionIsTerminalForFulfillmentAndAllowsRefundRequestOnlyAfterPayment() {
        var beforePayment = rejected();
        assertThat(beforePayment.status()).isEqualTo(OrderStatus.REJECTED);
        denied(beforePayment, OrderWorkflowAction.REFUND_REQUESTED, o -> o.recordRefundRequest(NOW));
        var rejectedAfterPayment = paid(placed()).recordRestaurantRejection(NOW);
        var requested = rejectedAfterPayment.recordRefundRequest(NOW);
        assertThat(requested.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(requested.workflowOccurrences()).hasSize(3);
        denied(requested, OrderWorkflowAction.REFUND_REQUESTED, o -> o.recordRefundRequest(NOW));
        denied(requested, OrderWorkflowAction.ORDER_COMPLETED, o -> o.recordCompletion(NOW));
    }

    @Test
    void cancellationBeforePaymentRemainsCancelledWithoutRefundRequest() {
        var cancelled = cancelled();
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.workflowOccurrences()).hasSize(1);
        denied(cancelled, OrderWorkflowAction.REFUND_REQUESTED, o -> o.recordRefundRequest(NOW));
    }

    @Test
    void cancellationAfterPaymentCanRecordOneRefundRequestButNeverResumeFulfillment() {
        var cancelled = paid(placed()).recordCancellation(NOW);
        var refundedRequested = cancelled.recordRefundRequest(NOW);
        assertThat(refundedRequested.status()).isEqualTo(OrderStatus.CANCELLED);
        denied(refundedRequested, OrderWorkflowAction.PREPARATION_STARTED, o -> o.recordPreparationStarted(NOW));
        denied(refundedRequested, OrderWorkflowAction.REFUND_REQUESTED, o -> o.recordRefundRequest(NOW));
    }

    @Test
    void rejectedOrderCannotCompleteEvenIfPaymentWasRecorded() {
        denied(paid(placed()).recordRestaurantRejection(NOW), OrderWorkflowAction.ORDER_COMPLETED,
                o -> o.recordCompletion(NOW));
    }

    @Test
    void completedOrderCannotBeReacceptedOrCancelled() {
        var terminal = completed();
        denied(terminal, OrderWorkflowAction.RESTAURANT_ACCEPTED, o -> o.recordRestaurantAcceptance(NOW));
        denied(terminal, OrderWorkflowAction.ORDER_CANCELLED, o -> o.recordCancellation(NOW));
        denied(terminal, OrderWorkflowAction.PAYMENT_RECORDED, o -> o.recordPayment(NOW));
    }

    @Test
    void preparingBeforeAcceptanceAndPaymentIsDenied() {
        denied(placed(), OrderWorkflowAction.PREPARATION_STARTED, o -> o.recordPreparationStarted(NOW));
        denied(accepted(placed()), OrderWorkflowAction.PREPARATION_STARTED, o -> o.recordPreparationStarted(NOW));
    }

    @Test
    void acceptedOrderCannotBeRejectedAndNoRejectedFactIsAdded() {
        var current = accepted(paid(placed()));
        denied(current, OrderWorkflowAction.RESTAURANT_REJECTED, o -> o.recordRestaurantRejection(NOW));
    }

    @Test
    void duplicatePaymentAndDuplicateAcceptanceAreDenied() {
        denied(paid(placed()), OrderWorkflowAction.PAYMENT_RECORDED, o -> o.recordPayment(NOW));
        denied(accepted(placed()), OrderWorkflowAction.RESTAURANT_ACCEPTED,
                o -> o.recordRestaurantAcceptance(NOW));
    }

    @Test
    void paymentAfterCancellationAndPreparationIsDenied() {
        denied(cancelled(), OrderWorkflowAction.PAYMENT_RECORDED, o -> o.recordPayment(NOW));
        denied(preparing(accepted(paid(placed()))), OrderWorkflowAction.PAYMENT_RECORDED,
                o -> o.recordPayment(NOW));
    }

    @Test
    void impossibleSequenceCannotBeRecordedAndEarlierObjectRemainsUnchanged() {
        var current = placed();
        denied(current, OrderWorkflowAction.ORDER_COMPLETED, o -> o.recordCompletion(NOW));
        assertThat(current.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(current.workflowOccurrences()).isEmpty();
    }

    private void denied(
            Order source,
            OrderWorkflowAction action,
            Function<Order, Order> operation
    ) {
        var before = source.workflowOccurrences();
        assertThatThrownBy(() -> operation.apply(source))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .satisfies(error -> {
                    var rejected = (InvalidOrderTransitionException) error;
                    assertThat(rejected.source()).isEqualTo(source.status());
                    assertThat(rejected.action()).isEqualTo(action);
                });
        assertThat(source.workflowOccurrences()).containsExactlyElementsOf(before);
    }
}
