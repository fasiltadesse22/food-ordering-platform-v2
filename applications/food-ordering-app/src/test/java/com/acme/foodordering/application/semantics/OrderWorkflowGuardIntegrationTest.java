package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.RestaurantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWorkflowGuardIntegrationTest {

    @Test
    void guardFailureBecomesBusinessRejectionAndDoesNotReplaceRepositoryState() {
        var repository = new InMemoryOrderRepository();
        var clock = Clock.fixed(
                Instant.parse("2026-10-08T08:45:00Z"),
                ZoneOffset.UTC
        );
        var placeOrder = new PlaceOrderService(repository, clock);
        var workflow = new OrderWorkflowService(repository, clock);

        var placedResult = placeOrder.place(new PlaceOrderCommand(
                "customer-1",
                "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "item-1",
                        "Guard Meal",
                        1,
                        new BigDecimal("9.00")
                ))
        ));

        assertThat(placedResult).isInstanceOf(PlaceOrderResult.Accepted.class);
        var orderId = ((PlaceOrderResult.Accepted) placedResult).order().id();
        var before = repository.findCurrentById(orderId).orElseThrow();

        var result = workflow.recordRestaurantAcceptance(
                orderId,
                new RestaurantId("restaurant-2")
        );

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(
                        OrderActionRejection.Code.RESTAURANT_DOES_NOT_OWN_ORDER
                );

        var after = repository.findCurrentById(orderId).orElseThrow();

        assertThat(after).isSameAs(before);
        assertThat(after.status().name()).isEqualTo("PLACED");
        assertThat(after.workflowOccurrences()).isEmpty();
    }
}
