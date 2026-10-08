package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTerminalityAndCompensationTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-1");

    @Test
    void rejectedCancelledAndCompletedAreExplicitLifecycleTerminalStates() {
        assertThat(OrderStatus.REJECTED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.COMPLETED.isTerminal()).isTrue();

        assertThat(OrderStatus.PLACED.isTerminal()).isFalse();
        assertThat(OrderStatus.ACCEPTED.isTerminal()).isFalse();
        assertThat(OrderStatus.PREPARING.isTerminal()).isFalse();
    }

    @Test
    void refundIsCompensationThatPreservesTheOriginalPaymentFact() {
        var paid = placedOrder().recordPayment(NOW);
        var cancelled = paid.recordCancellation(CUSTOMER, NOW.plusSeconds(1));
        var refundRequested = cancelled.recordRefundRequest(NOW.plusSeconds(2));

        assertThat(refundRequested.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(refundRequested.status().isTerminal()).isTrue();
        assertThat(refundRequested.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
    }

    @Test
    void secondRefundRequestIsRejectedWithoutErasingFirstCompensatingAction() {
        var firstRefund = placedOrder()
                .recordPayment(NOW)
                .recordCancellation(CUSTOMER, NOW.plusSeconds(1))
                .recordRefundRequest(NOW.plusSeconds(2));

        assertThatThrownBy(() -> firstRefund.recordRefundRequest(NOW.plusSeconds(3)))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(OrderGuardViolationException.Code.REFUND_ALREADY_REQUESTED));

        assertThat(firstRefund.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
    }

    @Test
    void completedOrderCannotBeCancelled() {
        var completed = placedOrder()
                .recordRestaurantAcceptance(RESTAURANT, NOW)
                .recordPreparationStarted(RESTAURANT, NOW.plusSeconds(1))
                .recordCompletion(RESTAURANT, NOW.plusSeconds(2));

        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.status().isTerminal()).isTrue();

        assertThatThrownBy(() -> completed.recordCancellation(
                CUSTOMER,
                NOW.plusSeconds(3)
        ))
                .isInstanceOf(IllegalOrderTransitionException.class)
                .hasMessageContaining("CANCEL")
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void terminalCancelledOrderCannotBeModified() {
        var cancelled = placedOrder().recordCancellation(CUSTOMER, NOW);

        assertThatThrownBy(() -> cancelled.modifyLines(
                CUSTOMER,
                replacementLines(),
                NOW.plusSeconds(1)
        ))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code.MODIFICATION_REQUIRES_PLACED_ORDER
                ));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test
    void placedOrderCanBeModifiedWithoutChangingLifecycleState() {
        var placed = placedOrder();

        var modified = placed.modifyLines(
                CUSTOMER,
                replacementLines(),
                NOW
        );

        assertThat(modified.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(modified.lines()).isEqualTo(replacementLines());
        assertThat(modified.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED);
        assertThat(placed.lines()).isNotEqualTo(modified.lines());
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from("123e4567-e89b-12d3-a456-426614174000"),
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                )),
                NOW.minusSeconds(30)
        );
    }

    private static List<OrderLine> replacementLines() {
        return List.of(new OrderLine(
                "pizza-1",
                "Margherita",
                2,
                new BigDecimal("7.00")
        ));
    }
}
