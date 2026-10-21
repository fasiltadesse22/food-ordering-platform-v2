package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionContractTest {
    private static final Instant AT = Instant.parse("2026-10-21T05:00:00Z");

    private Order placed() {
        return Order.place(OrderId.random(), new CustomerId("customer"),
                new RestaurantId("restaurant"),
                List.of(new OrderLine("item", "Meal", 2, new BigDecimal("10.00"))), AT);
    }

    @Test
    void preconditionRequiresAcceptanceBeforePreparing() {
        var current = placed().recordPayment(AT);
        assertThatThrownBy(() -> OrderTransitionContract.requirePreconditions(
                current, OrderWorkflowAction.PREPARATION_STARTED))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .hasMessageContaining("requires source state ACCEPTED");
    }

    @Test
    void preconditionRequiresRecordedPaymentBeforePreparing() {
        var accepted = placed().recordRestaurantAcceptance(AT);
        assertThatThrownBy(() -> OrderTransitionContract.requirePreconditions(
                accepted, OrderWorkflowAction.PREPARATION_STARTED))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .hasMessageContaining("requires recorded payment");
    }

    @Test
    void successfulGuardSelectsTargetWithoutMutatingSourceOrRecordingOccurrence() {
        var before = placed();
        var expected = OrderTransitionContract.requirePreconditions(
                before, OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(expected).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(before.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(before.workflowOccurrences()).isEmpty();
    }

    @Test
    void legalResultSatisfiesPostconditionsAndCopiesOnlyOneNewFact() {
        var before = placed().recordPayment(AT);
        var after = before.recordRestaurantAcceptance(AT);
        OrderTransitionContract.requirePostconditions(before, after,
                OrderWorkflowAction.RESTAURANT_ACCEPTED,
                WorkflowParticipant.RESTAURANT_OPERATOR, AT, OrderStatus.ACCEPTED);
        assertThat(after.workflowOccurrences()).hasSize(before.workflowOccurrences().size() + 1);
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.total()).isEqualByComparingTo(before.total());
    }

    @Test
    void postconditionRejectsIncorrectTargetStatusEvenIfHistoryAdvanced() {
        var before = placed();
        var after = before.recordPayment(AT); // appends a fact, remains PLACED
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT, AT, OrderStatus.COMPLETED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("target state");
    }

    @Test
    void postconditionRejectsMissingAcceptedOccurrence() {
        var unchanged = placed();
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                unchanged, unchanged, OrderWorkflowAction.RESTAURANT_ACCEPTED,
                WorkflowParticipant.RESTAURANT_OPERATOR, AT, OrderStatus.PLACED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void postconditionRejectsWrongBusinessIdentity() {
        var before = placed();
        var unrelated = placed();
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, unrelated, OrderWorkflowAction.RESTAURANT_ACCEPTED,
                WorkflowParticipant.RESTAURANT_OPERATOR, AT, OrderStatus.PLACED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("OrderId changed");
    }

    @Test
    void postconditionRejectsWrongActionInAcceptedHistory() {
        var before = placed();
        var after = before.recordPayment(AT);
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.RESTAURANT_ACCEPTED,
                WorkflowParticipant.PAYMENT_PARTICIPANT, AT, OrderStatus.PLACED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("recorded action");
    }

    @Test
    void postconditionRejectsWrongParticipantOrTimestamp() {
        var before = placed();
        var after = before.recordPayment(AT);
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.CUSTOMER, AT, OrderStatus.PLACED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("participant");
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT, AT.plusSeconds(1), OrderStatus.PLACED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void businessDenialIsDistinctFromDeveloperPostconditionBreach() {
        var before = placed();
        assertThatThrownBy(() -> before.recordCompletion(AT))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .isNotInstanceOf(OrderTransitionContractViolationException.class);

        var after = before.recordPayment(AT);
        assertThatThrownBy(() -> OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.PAYMENT_RECORDED,
                WorkflowParticipant.PAYMENT_PARTICIPANT, AT, OrderStatus.ACCEPTED))
                .isInstanceOf(OrderTransitionContractViolationException.class)
                .isNotInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    void cancellationPostconditionEnsuresNoPreparationOccurrenceWasAdded() {
        var before = placed().recordRestaurantAcceptance(AT);
        var after = before.recordCancellation(AT);
        assertThat(after.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(after.workflowOccurrences()).noneMatch(
                occurrence -> occurrence.action() == OrderWorkflowAction.PREPARATION_STARTED);
        assertThatThrownBy(() -> after.recordPreparationStarted(AT))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    void sameDomainContractWorksForConditionalRefundWithoutChangingFulfillmentState() {
        var before = placed().recordPayment(AT).recordRestaurantRejection(AT);
        var after = before.recordRefundRequest(AT);
        OrderTransitionContract.requirePostconditions(
                before, after, OrderWorkflowAction.REFUND_REQUESTED,
                WorkflowParticipant.PLATFORM, AT, OrderStatus.REJECTED);
        assertThat(after.status()).isEqualTo(OrderStatus.REJECTED);
    }
}
