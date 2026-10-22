package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderClosureAndIrreversibilityTest {
    private static final Instant T = Instant.parse("2026-10-22T10:00:00Z");
    private Order placed() {
        return Order.place(OrderId.random(), new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine("burger", "Burger", 1, new BigDecimal("12.00"))), T);
    }
    private Order paid(Order o) { return o.recordPayment(T); }
    private Order done() {
        return paid(placed()).recordRestaurantAcceptance(T)
                .recordPreparationStarted(T).recordCompletion(T);
    }
    @Test void placedAcceptedPreparingAreNotTerminal() {
        var x = placed();
        assertThat(x.closure().fulfillmentTerminal()).isFalse();
        var y = paid(x).recordRestaurantAcceptance(T);
        assertThat(y.closure().fulfillmentTerminal()).isFalse();
        assertThat(y.recordPreparationStarted(T).closure().fulfillmentTerminal()).isFalse();
    }
    @Test void cancellationWithoutPaymentHasNoLocalRefundBasis() {
        var x = placed().recordCancellation(T);
        assertThat(x.closure().fulfillment()).isEqualTo(OrderClosure.FulfillmentClosure.CUSTOMER_CANCELLED);
        assertThat(x.closure().fulfillmentTerminal()).isTrue();
        assertThat(x.closure().refundRequest()).isEqualTo(OrderClosure.RefundRequestReadiness.NO_LOCAL_PAYMENT_EVIDENCE);
        assertThatThrownBy(() -> x.recordRefundRequest(T)).isInstanceOf(InvalidOrderTransitionException.class);
    }
    @Test void paidRejectionAddsRefundRequestWithoutErasingPaymentFact() {
        var x = paid(placed()).recordRestaurantRejection(T);
        assertThat(x.closure().refundRequest()).isEqualTo(OrderClosure.RefundRequestReadiness.MAY_RECORD_REQUEST);
        var y = x.recordRefundRequest(T);
        assertThat(y.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(y.closure().refundRequest()).isEqualTo(OrderClosure.RefundRequestReadiness.REQUEST_ALREADY_RECORDED);
        assertThat(y.workflowOccurrences()).extracting(t -> t.action()).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED, OrderWorkflowAction.RESTAURANT_REJECTED,
                OrderWorkflowAction.REFUND_REQUESTED);
        assertThat(x.workflowOccurrences()).hasSize(2);
    }
    @Test void paidCancellationKeepsTerminalStateAndBothFacts() {
        var x = paid(placed()).recordCancellation(T).recordRefundRequest(T);
        assertThat(x.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(x.closure().fulfillmentTerminal()).isTrue();
        assertThat(x.workflowOccurrences()).extracting(t -> t.action()).containsExactly(
                OrderWorkflowAction.PAYMENT_RECORDED, OrderWorkflowAction.ORDER_CANCELLED,
                OrderWorkflowAction.REFUND_REQUESTED);
    }
    @Test void refundBeforePaymentDeniedWithNoNewFact() {
        var x = placed().recordCancellation(T);
        assertThatThrownBy(() -> x.recordRefundRequest(T)).isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(x.workflowOccurrences()).hasSize(1);
        assertThat(x.closure().refundRequestRecorded()).isFalse();
    }
    @Test void duplicateRefundDeniedAndExactlyOneRequestRemains() {
        var x = paid(placed()).recordCancellation(T).recordRefundRequest(T);
        assertThatThrownBy(() -> x.recordRefundRequest(T)).isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(x.workflowOccurrences()).filteredOn(t -> t.action() == OrderWorkflowAction.REFUND_REQUESTED).hasSize(1);
    }
    @Test void completedOrderCannotBeCancelledOrReopened() {
        var x = done();
        assertThat(x.closure().fulfillment()).isEqualTo(OrderClosure.FulfillmentClosure.FULFILLMENT_COMPLETED);
        assertThat(x.closure().permitsAutomaticFulfillmentReopen()).isFalse();
        assertThatThrownBy(() -> x.recordCancellation(T)).isInstanceOf(InvalidOrderTransitionException.class);
        assertThatThrownBy(() -> x.recordRestaurantAcceptance(T)).isInstanceOf(InvalidOrderTransitionException.class);
    }
    @Test void rejectedAndCancelledCannotResumePreparationOrCompletion() {
        for (var x : List.of(paid(placed()).recordRestaurantRejection(T),
                             paid(placed()).recordCancellation(T))) {
            assertThat(x.closure().fulfillmentTerminal()).isTrue();
            assertThatThrownBy(() -> x.recordPreparationStarted(T)).isInstanceOf(InvalidOrderTransitionException.class);
            assertThatThrownBy(() -> x.recordCompletion(T)).isInstanceOf(InvalidOrderTransitionException.class);
        }
    }
    @Test void terminalOrderExposedCollectionsAreNotEditable() {
        var x = done();
        var beforeLines = List.copyOf(x.lines());
        var beforeHistory = List.copyOf(x.workflowOccurrences());
        assertThatThrownBy(() -> x.lines().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> x.workflowOccurrences().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(x.lines()).containsExactlyElementsOf(beforeLines);
        assertThat(x.workflowOccurrences()).containsExactlyElementsOf(beforeHistory);
    }
    @Test void refundRequestIsANewFactNotAPaymentReversal() {
        var x = paid(placed()).recordRestaurantRejection(T).recordRefundRequest(T);
        assertThat(x.closure().paymentRecorded()).isTrue();
        assertThat(x.closure().refundRequestRecorded()).isTrue();
        assertThat(x.status()).isEqualTo(OrderStatus.REJECTED);
    }
    @Test void completedOrderHasNoAutomaticRefundRequestPath() {
        var x = done();
        assertThat(x.closure().refundRequest()).isEqualTo(OrderClosure.RefundRequestReadiness.NOT_A_CLOSED_REFUND_PATH);
        assertThatThrownBy(() -> x.recordRefundRequest(T)).isInstanceOf(InvalidOrderTransitionException.class);
    }
    @Test void readOnlyClosureAssessmentDoesNotMutateAnOrder() {
        var x = paid(placed()).recordCancellation(T);
        var before = x.workflowOccurrences().size();
        assertThat(x.closure()).isEqualTo(x.closure());
        assertThat(x.workflowOccurrences()).hasSize(before);
        assertThat(x.status()).isEqualTo(OrderStatus.CANCELLED);
    }
}
