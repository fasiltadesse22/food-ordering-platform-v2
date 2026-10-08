package com.acme.foodordering.domain.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private static final Instant NOW = Instant.parse("2026-10-08T07:00:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-1");

    @Test
    void legalFulfillmentPathIsPlacedToAcceptedToPreparingToCompleted() {
        var placed = placedOrder();

        var accepted = placed.recordRestaurantAcceptance(RESTAURANT, NOW);
        var preparing = accepted.recordPreparationStarted(RESTAURANT, NOW.plusSeconds(1));
        var completed = preparing.recordCompletion(RESTAURANT, NOW.plusSeconds(2));

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(preparing.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void placedOrderCanBranchToRejected() {
        var rejected = placedOrder().recordRestaurantRejection(RESTAURANT, NOW);

        assertThat(rejected.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void placedOrderCanBranchToCancelled() {
        var cancelled = placedOrder().recordCancellation(CUSTOMER, NOW);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentMilestoneDoesNotCreateASecondOrderLifecycleDimensionInsideStatus() {
        var paid = placedOrder().recordPayment(NOW);

        assertThat(paid.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void preparationFromPlacedIsIllegalAndLeavesOriginalRepresentationUnchanged() {
        var placed = placedOrder();

        assertThatThrownBy(() -> placed.recordPreparationStarted(RESTAURANT, NOW))
                .isInstanceOf(IllegalOrderTransitionException.class)
                .hasMessageContaining("START_PREPARATION")
                .hasMessageContaining("PLACED");

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.workflowOccurrences()).isEmpty();
    }

    @Test
    void rejectionAfterAcceptanceIsIllegal() {
        var accepted = placedOrder().recordRestaurantAcceptance(RESTAURANT, NOW);

        assertThatThrownBy(() -> accepted.recordRestaurantRejection(
                RESTAURANT,
                NOW.plusSeconds(1)
        ))
                .isInstanceOf(IllegalOrderTransitionException.class)
                .hasMessageContaining("REJECT")
                .hasMessageContaining("ACCEPTED");

        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void completionBeforePreparationIsIllegal() {
        var accepted = placedOrder().recordRestaurantAcceptance(RESTAURANT, NOW);

        assertThatThrownBy(() -> accepted.recordCompletion(
                RESTAURANT,
                NOW.plusSeconds(1)
        ))
                .isInstanceOf(IllegalOrderTransitionException.class)
                .hasMessageContaining("COMPLETE")
                .hasMessageContaining("ACCEPTED");

        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
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
                NOW.minusSeconds(10)
        );
    }
}
