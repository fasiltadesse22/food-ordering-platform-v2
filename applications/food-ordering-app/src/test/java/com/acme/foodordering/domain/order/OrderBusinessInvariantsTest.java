package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P10 distinct semantic/invariant suite. Intentionally constructs syntactically
 * and structurally valid snapshots that violate the accepted lifecycle.
 * A hypothetical NOT NULL / status enum CHECK cannot detect these histories.
 */
class OrderBusinessInvariantsTest {
    private static final Instant AT = Instant.parse("2026-10-28T00:00:00Z");

    private OrderWorkflowOccurrence o(OrderWorkflowAction action) {
        WorkflowParticipant actor = switch (action) {
            case PAYMENT_RECORDED -> WorkflowParticipant.PAYMENT_PARTICIPANT;
            case RESTAURANT_ACCEPTED, RESTAURANT_REJECTED,
                 PREPARATION_STARTED, ORDER_COMPLETED -> WorkflowParticipant.RESTAURANT_OPERATOR;
            case ORDER_CANCELLED -> WorkflowParticipant.CUSTOMER;
            case REFUND_REQUESTED -> WorkflowParticipant.PLATFORM;
        };
        return new OrderWorkflowOccurrence(action, actor, AT);
    }

    private List<OrderWorkflowOccurrence> h(OrderWorkflowAction... actions) {
        return java.util.Arrays.stream(actions).map(this::o).toList();
    }

    @Test void emptyHistoryAndPlacedIsValid() {
        assertThatCode(() -> OrderBusinessInvariants.requireValid(OrderStatus.PLACED, List.of()))
            .doesNotThrowAnyException();
    }

    @Test void acceptedOrderWithLocalPaymentBeforeAcceptanceIsValid() {
        assertThatCode(() -> OrderBusinessInvariants.requireValid(OrderStatus.ACCEPTED,
                h(OrderWorkflowAction.PAYMENT_RECORDED, OrderWorkflowAction.RESTAURANT_ACCEPTED)))
            .doesNotThrowAnyException();
    }

    @Test void recordedPaymentAfterAcceptanceBeforePreparationIsValid() {
        assertThatCode(() -> OrderBusinessInvariants.requireValid(OrderStatus.PREPARING,
                h(OrderWorkflowAction.RESTAURANT_ACCEPTED,
                  OrderWorkflowAction.PAYMENT_RECORDED,
                  OrderWorkflowAction.PREPARATION_STARTED))).doesNotThrowAnyException();
    }

    @Test void terminalCancellationWithOneRefundRequestPreservesHistory() {
        assertThatCode(() -> OrderBusinessInvariants.requireValid(OrderStatus.CANCELLED,
                h(OrderWorkflowAction.PAYMENT_RECORDED,
                  OrderWorkflowAction.ORDER_CANCELLED,
                  OrderWorkflowAction.REFUND_REQUESTED))).doesNotThrowAnyException();
    }

    @Test void terminalCompletionFromLegalHistoryIsValid() {
        assertThatCode(() -> OrderBusinessInvariants.requireValid(OrderStatus.COMPLETED,
                h(OrderWorkflowAction.PAYMENT_RECORDED,
                  OrderWorkflowAction.RESTAURANT_ACCEPTED,
                  OrderWorkflowAction.PREPARATION_STARTED,
                  OrderWorkflowAction.ORDER_COMPLETED))).doesNotThrowAnyException();
    }

    @Test void validNonNullCancelledStatusWithPreparationAfterCancellationViolatesTemporalInvariant() {
        assertInvalid(OrderStatus.PREPARING,
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.ORDER_CANCELLED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.PREPARATION_STARTED);
    }

    @Test void statusAloneCanBeAValidEnumYetContradictHistoricalTruth() {
        assertInvalid(OrderStatus.COMPLETED, OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test void preparingStatusWithMissingPaymentHistoryIsInvalidDespiteValidFields() {
        assertInvalid(OrderStatus.PREPARING,
                OrderWorkflowAction.RESTAURANT_ACCEPTED, OrderWorkflowAction.PREPARATION_STARTED);
    }

    @Test void completedWithoutPreparationIsInvalidDespiteValidStatusEnum() {
        assertInvalid(OrderStatus.COMPLETED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED, OrderWorkflowAction.ORDER_COMPLETED);
    }

    @Test void duplicateAcceptedLocalPaymentIsInvalid() {
        assertInvalid(OrderStatus.PLACED,
                OrderWorkflowAction.PAYMENT_RECORDED, OrderWorkflowAction.PAYMENT_RECORDED);
    }

    @Test void refundWithoutLocalPaymentIsInvalidEvenOnCancelledOrder() {
        assertInvalid(OrderStatus.CANCELLED,
                OrderWorkflowAction.ORDER_CANCELLED, OrderWorkflowAction.REFUND_REQUESTED);
    }

    @Test void duplicateRefundRequestIsInvalidInAnOtherwiseClosedOrder() {
        assertInvalid(OrderStatus.CANCELLED,
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.ORDER_CANCELLED,
                OrderWorkflowAction.REFUND_REQUESTED,
                OrderWorkflowAction.REFUND_REQUESTED);
    }

    @Test void afterCompletionASecondFulfillmentActionIsInvalid() {
        assertInvalid(OrderStatus.CANCELLED,
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.PREPARATION_STARTED,
                OrderWorkflowAction.ORDER_COMPLETED,
                OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test void correctlyTypedParticipantCanStillRepresentWrongBusinessHistory() {
        assertInvalid(OrderStatus.REJECTED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.RESTAURANT_REJECTED);
    }

    @Test void wrongModeledParticipantViolatesOccurrenceSemanticIntegrity() {
        var structurallyValid = new OrderWorkflowOccurrence(
                OrderWorkflowAction.RESTAURANT_REJECTED,
                WorkflowParticipant.CUSTOMER, AT);
        assertThatThrownBy(() ->
                OrderBusinessInvariants.requireValid(OrderStatus.REJECTED, List.of(structurallyValid)))
            .isInstanceOf(OrderInvariantViolationException.class)
            .hasMessageContaining("participant");
    }

    @Test void inferredStatusMustEqualClaimedStatus() {
        assertInvalid(OrderStatus.REJECTED, OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void noRefundRequestCanFollowCompletedFulfillmentUnderCurrentPolicy() {
        assertInvalid(OrderStatus.COMPLETED,
                OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                OrderWorkflowAction.PREPARATION_STARTED,
                OrderWorkflowAction.ORDER_COMPLETED,
                OrderWorkflowAction.REFUND_REQUESTED);
    }

    private void assertInvalid(OrderStatus claimed, OrderWorkflowAction... history) {
        var structurallyValid = h(history);
        // Every record has non-null action/participant/instant and a valid enum.
        assertThatThrownBy(() -> OrderBusinessInvariants.requireValid(claimed, structurallyValid))
            .isInstanceOf(OrderInvariantViolationException.class);
    }
}
