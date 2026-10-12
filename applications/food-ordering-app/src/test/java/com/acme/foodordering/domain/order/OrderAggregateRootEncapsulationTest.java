package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAggregateRootEncapsulationTest {
    private static final Instant T = Instant.parse("2026-10-12T12:00:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-root");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-root");

    private static Order placed() {
        return Order.place(
                OrderId.from("123e4567-e89b-12d3-a456-426614174107"),
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine("burger", "Burger", 2, new BigDecimal("5.50"))),
                T
        );
    }

    private static OrderWorkflowOccurrence occurrence(OrderWorkflowAction action) {
        return new OrderWorkflowOccurrence(
                action,
                WorkflowParticipant.RESTAURANT_OPERATOR,
                T.plusSeconds(1)
        );
    }

    @Test
    void acceptanceFactCannotBeRepresentedAsCurrentPlacedOrder() {
        assertThatThrownBy(() -> OrderInvariants.verify(
                OrderStatus.PLACED,
                List.of(occurrence(OrderWorkflowAction.RESTAURANT_ACCEPTED))
        )).isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(OrderInvariantViolationException.Code
                        .ACCEPTANCE_FACT_REQUIRES_ACCEPTED_OR_LATER_STATE));
    }

    @Test
    void acceptanceFactCannotBeCombinedWithCurrentlyRejectedOrCancelledState() {
        assertThatThrownBy(() -> OrderInvariants.verify(
                OrderStatus.REJECTED,
                List.of(
                        occurrence(OrderWorkflowAction.RESTAURANT_ACCEPTED),
                        occurrence(OrderWorkflowAction.RESTAURANT_REJECTED)
                )
        )).isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(OrderInvariantViolationException.Code
                        .ACCEPTANCE_FACT_REQUIRES_ACCEPTED_OR_LATER_STATE));
        assertThatThrownBy(() -> OrderInvariants.verify(
                OrderStatus.CANCELLED,
                List.of(
                        occurrence(OrderWorkflowAction.RESTAURANT_ACCEPTED),
                        occurrence(OrderWorkflowAction.ORDER_CANCELLED)
                )
        )).isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(OrderInvariantViolationException.Code
                        .ACCEPTANCE_FACT_REQUIRES_ACCEPTED_OR_LATER_STATE));
    }

    @Test
    void publicRootLifecycleStillConstructsLegalAcceptedPreparingAndCompletedStates() {
        var p = placed();
        var a = p.recordRestaurantAcceptance(RESTAURANT, T.plusSeconds(1));
        var r = a.recordPreparationStarted(RESTAURANT, T.plusSeconds(2));
        var c = r.recordCompletion(RESTAURANT, T.plusSeconds(3));
        assertThat(a.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(r.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(c.status()).isEqualTo(OrderStatus.COMPLETED);
        for (var candidate : List.of(a, r, c)) {
            OrderInvariants.verify(candidate.status(), candidate.workflowOccurrences());
            assertThat(candidate.id()).isEqualTo(p.id());
        }
        assertThat(p.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(p.workflowOccurrences()).isEmpty();
    }

    @Test
    void externalCallersCannotMutateOwnedLineOrHistoryCollections() {
        var callerLines = new ArrayList<>(placed().lines());
        var p = Order.place(
                OrderId.from("123e4567-e89b-12d3-a456-426614174108"),
                CUSTOMER, RESTAURANT, callerLines, T
        );
        callerLines.clear();
        assertThat(p.lines()).hasSize(1);
        assertThatThrownBy(() -> p.lines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        var accepted = p.recordRestaurantAcceptance(RESTAURANT, T.plusSeconds(1));
        assertThatThrownBy(() -> accepted.workflowOccurrences().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(accepted.workflowOccurrences()).hasSize(1);
    }

    @Test
    void illegalTransitionAndWrongActorCannotCreatePublishedSuccessor() {
        var p = placed();
        assertThatThrownBy(() -> p.recordCompletion(RESTAURANT, T.plusSeconds(1)))
                .isInstanceOf(IllegalOrderTransitionException.class);
        assertThatThrownBy(() -> p.recordRestaurantAcceptance(
                new RestaurantId("different-restaurant"), T.plusSeconds(1)
        )).isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(OrderGuardViolationException.Code.RESTAURANT_DOES_NOT_OWN_ORDER));
        assertThat(p.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(p.workflowOccurrences()).isEmpty();
    }

    @Test
    void refundRootGuardRejectsSecondRequestWhilePreservingPreviousState() {
        var cancelled = placed().recordPayment(T.plusSeconds(1))
                .recordCancellation(CUSTOMER, T.plusSeconds(2));
        var first = cancelled.recordRefundRequest(T.plusSeconds(3));
        assertThat(first.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> first.recordRefundRequest(T.plusSeconds(4)))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(OrderGuardViolationException.Code.REFUND_ALREADY_REQUESTED));
        assertThat(cancelled.workflowOccurrences()).hasSize(2);
        assertThat(first.workflowOccurrences()).hasSize(3);
    }

    @Test
    void negativeControlLocalPaymentFactCurrentlyAllowsRepetition() {
        var p = placed();
        var once = p.recordPayment(T.plusSeconds(1));
        var twice = once.recordPayment(T.plusSeconds(2));
        assertThat(twice.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.PAYMENT_RECORDED
                );
        // Intentional negative control: the existing public API permits duplicate
        // local payment facts. Do not infer financial idempotency or external truth.
    }
}
