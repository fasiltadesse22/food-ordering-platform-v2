package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderInvariantAndValidationTest {

    private static final Instant NOW =
            Instant.parse("2026-10-08T12:00:00Z");

    @Test
    void blankIdentifiersFailStructuralValidationBeforeBusinessRules() {
        assertThatThrownBy(() -> new CustomerId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer id");

        assertThatThrownBy(() -> new RestaurantId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restaurant id");
    }

    @Test
    void invalidLineShapeFailsValidationWithoutNeedingOrderLifecycleContext() {
        assertThatThrownBy(() -> new OrderLine(
                "burger-1",
                "Classic Burger",
                0,
                new BigDecimal("5.50")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");

        assertThatThrownBy(() -> new OrderLine(
                "burger-1",
                "Classic Burger",
                1,
                new BigDecimal("-0.01")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit price");
    }

    @Test
    void structurallyValidFactsCanStillFormAnInvariantInvalidOrderHistory() {
        var occurrences = List.of(
                new OrderWorkflowOccurrence(
                        OrderWorkflowAction.ORDER_CANCELLED,
                        WorkflowParticipant.CUSTOMER,
                        NOW
                ),
                new OrderWorkflowOccurrence(
                        OrderWorkflowAction.PREPARATION_STARTED,
                        WorkflowParticipant.RESTAURANT_OPERATOR,
                        NOW.plusSeconds(1)
                )
        );

        assertThatThrownBy(() -> OrderInvariants.verify(
                OrderStatus.PREPARING,
                occurrences
        ))
                .isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(
                        OrderInvariantViolationException.Code
                                .CANCELLED_FACT_REQUIRES_CANCELLED_STATE
                ));
    }

    @Test
    void compensationHistoryCanEvolveWhileCancelledInvariantStillHolds() {
        var order = placedOrder()
                .recordPayment(NOW)
                .recordCancellation(
                        new CustomerId("customer-1"),
                        NOW.plusSeconds(1)
                )
                .recordRefundRequest(NOW.plusSeconds(2));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);

        OrderInvariants.verify(
                order.status(),
                order.workflowOccurrences()
        );

        assertThat(order.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
    }

    @Test
    void publicTransitionRuleRejectsBusinessRequestBeforeInvariantCanBreak() {
        var cancelled = placedOrder().recordCancellation(
                new CustomerId("customer-1"),
                NOW
        );

        assertThatThrownBy(() -> cancelled.recordPreparationStarted(
                new RestaurantId("restaurant-1"),
                NOW.plusSeconds(1)
        ))
                .isInstanceOf(IllegalOrderTransitionException.class);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        OrderInvariants.verify(
                cancelled.status(),
                cancelled.workflowOccurrences()
        );
    }

    @Test
    void completedStateWithoutPreparationHistoryIsInvariantViolationEvenThoughEnumsAreValid() {
        var occurrences = List.of(
                new OrderWorkflowOccurrence(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        WorkflowParticipant.RESTAURANT_OPERATOR,
                        NOW
                ),
                new OrderWorkflowOccurrence(
                        OrderWorkflowAction.ORDER_COMPLETED,
                        WorkflowParticipant.RESTAURANT_OPERATOR,
                        NOW.plusSeconds(1)
                )
        );

        assertThatThrownBy(() -> OrderInvariants.verify(
                OrderStatus.COMPLETED,
                occurrences
        ))
                .isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(
                        OrderInvariantViolationException.Code
                                .COMPLETED_STATE_REQUIRES_ACCEPTANCE_PREPARATION_AND_COMPLETION_FACTS
                ));
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from("123e4567-e89b-12d3-a456-426614174000"),
                new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                )),
                NOW.minusSeconds(30)
        );
    }
}
