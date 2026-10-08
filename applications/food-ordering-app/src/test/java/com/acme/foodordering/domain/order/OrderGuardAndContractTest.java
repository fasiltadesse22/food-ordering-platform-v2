package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderGuardAndContractTest {

    private static final Instant NOW = Instant.parse("2026-10-08T08:30:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-1");
    private static final CustomerId OTHER_CUSTOMER = new CustomerId("customer-2");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-1");
    private static final RestaurantId OTHER_RESTAURANT = new RestaurantId("restaurant-2");

    @Test
    void structurallyLegalAcceptanceStillRequiresOwningRestaurant() {
        var placed = placedOrder();

        assertThatThrownBy(() -> placed.recordRestaurantAcceptance(
                OTHER_RESTAURANT,
                NOW
        ))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code.RESTAURANT_DOES_NOT_OWN_ORDER
                ));

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.workflowOccurrences()).isEmpty();
    }

    @Test
    void structurallyLegalCancellationStillRequiresOwningCustomer() {
        var placed = placedOrder();

        assertThatThrownBy(() -> placed.recordCancellation(
                OTHER_CUSTOMER,
                NOW
        ))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code.CUSTOMER_DOES_NOT_OWN_ORDER
                ));

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.workflowOccurrences()).isEmpty();
    }

    @Test
    void statePreconditionIsEvaluatedBeforeRestaurantOwnershipGuard() {
        var placed = placedOrder();

        assertThatThrownBy(() -> placed.recordPreparationStarted(
                OTHER_RESTAURANT,
                NOW
        ))
                .isInstanceOf(IllegalOrderTransitionException.class)
                .isNotInstanceOf(OrderGuardViolationException.class);
    }

    @Test
    void refundRequestRequiresRecordedPayment() {
        var cancelled = placedOrder().recordCancellation(CUSTOMER, NOW);

        assertThatThrownBy(() -> cancelled.recordRefundRequest(NOW.plusSeconds(1)))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code.REFUND_REQUIRES_RECORDED_PAYMENT
                ));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test
    void refundRequestRequiresRejectedOrCancelledLifecycleState() {
        var paid = placedOrder().recordPayment(NOW);
        var accepted = paid.recordRestaurantAcceptance(
                RESTAURANT,
                NOW.plusSeconds(1)
        );

        assertThatThrownBy(() -> accepted.recordRefundRequest(NOW.plusSeconds(2)))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code.REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER
                ));

        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
    }

    @Test
    void successfulAcceptanceSatisfiesIdentityStateAndOccurrencePostconditions() {
        var placed = placedOrder();

        var acceptedAt = NOW.plusSeconds(3);
        var accepted = placed.recordRestaurantAcceptance(
                RESTAURANT,
                acceptedAt
        );

        assertThat(accepted.id()).isEqualTo(placed.id());
        assertThat(accepted.customerId()).isEqualTo(placed.customerId());
        assertThat(accepted.restaurantId()).isEqualTo(placed.restaurantId());
        assertThat(accepted.lines()).isEqualTo(placed.lines());
        assertThat(accepted.placedAt()).isEqualTo(placed.placedAt());
        assertThat(accepted.total()).isEqualByComparingTo(placed.total());

        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.workflowOccurrences()).hasSize(
                placed.workflowOccurrences().size() + 1
        );

        var occurrence = accepted.workflowOccurrences().getLast();
        assertThat(occurrence.action())
                .isEqualTo(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(occurrence.occurredAt()).isEqualTo(acceptedAt);

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.workflowOccurrences()).isEmpty();
    }

    @Test
    void paidRejectedOrderCanRequestRefundWithoutChangingLifecycleState() {
        var paid = placedOrder().recordPayment(NOW);
        var rejected = paid.recordRestaurantRejection(
                RESTAURANT,
                NOW.plusSeconds(1)
        );

        var refundRequested = rejected.recordRefundRequest(NOW.plusSeconds(2));

        assertThat(refundRequested.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(refundRequested.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.RESTAURANT_REJECTED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
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
}
