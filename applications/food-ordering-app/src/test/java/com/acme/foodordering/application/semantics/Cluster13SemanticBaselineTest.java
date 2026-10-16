package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class Cluster13SemanticBaselineTest {
    private static final Instant AT = Instant.parse("2026-10-16T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AT, ZoneOffset.UTC);

    // Negative business-authority control, NOT proof that the domain Order is invalid:
    // both requests satisfy the existing contract, which does not consult Catalog.
    @Test
    void currentlyAcceptedPlacementDoesNotEstablishAnAuthoritativeCatalogPrice() {
        var repository = new InMemoryOrderRepository();
        var service = new PlaceOrderService(repository, CLOCK);

        var ordinary = (PlaceOrderResult.Accepted) service.place(command("5.50"));
        var different = (PlaceOrderResult.Accepted) service.place(command("0.01"));

        assertThat(ordinary.order().id()).isNotEqualTo(different.order().id());
        assertThat(ordinary.order().lines().getFirst().menuItemId()).isEqualTo("burger-1");
        assertThat(different.order().lines().getFirst().menuItemId()).isEqualTo("burger-1");
        assertThat(ordinary.order().lines().getFirst().unitPrice()).isEqualByComparingTo("5.50");
        assertThat(different.order().lines().getFirst().unitPrice()).isEqualByComparingTo("0.01");
        assertThat(ordinary.fact().total()).isEqualByComparingTo("11.00");
        assertThat(different.fact().total()).isEqualByComparingTo("0.02");
        assertThat(repository.size()).isEqualTo(2);
        assertThat(repository.findCurrentById(different.order().id())).isPresent();
    }

    // The existing guard proves Order's restaurant identity check, not
    // restaurant opening hours, personnel permissions or operational capacity.
    @Test
    void restaurantAcceptanceChecksAssignedIdentityAndLocalLifecycleOnly() {
        var order = placed();
        var accepted = order.recordRestaurantAcceptance(
                new RestaurantId("restaurant-1"), AT.plusSeconds(1));

        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.version()).isEqualTo(1);
        assertThat(accepted.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThatThrownBy(() -> order.recordRestaurantAcceptance(
                new RestaurantId("restaurant-2"), AT.plusSeconds(1)))
                .isInstanceOfSatisfying(OrderGuardViolationException.class,
                        failure -> assertThat(failure.code()).isEqualTo(
                                OrderGuardViolationException.Code.RESTAURANT_DOES_NOT_OWN_ORDER));
    }

    // A local milestone is not a financial-provider capture or settlement.
    @Test
    void localPaymentOccurrenceDoesNotChangeOrderingLifecycleStatus() {
        var placed = placed();
        var recorded = placed.recordPayment(AT.plusSeconds(2));

        assertThat(recorded.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(recorded.version()).isEqualTo(1);
        assertThat(recorded.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
        assertThat(placed.version()).isZero();
        assertThat(placed.workflowOccurrences()).isEmpty();
    }

    private static PlaceOrderCommand command(String price) {
        return new PlaceOrderCommand(
                "customer-1", "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "burger-1", "Classic Burger", 2, new BigDecimal(price))));
    }

    private static Order placed() {
        return Order.place(
                OrderId.random(), new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine("burger-1", "Classic Burger", 2,
                        new BigDecimal("5.50"))), AT);
    }
}
