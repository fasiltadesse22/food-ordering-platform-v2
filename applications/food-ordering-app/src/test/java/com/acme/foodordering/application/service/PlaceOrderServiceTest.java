package com.acme.foodordering.application.service;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderServiceTest {

    @Test
    void persistsThePlacedOrderAndReturnsAnAcceptedResultWithTheResultingFact() {
        var repository = new InMemoryOrderRepository();
        var clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
        var service = new PlaceOrderService(repository, clock);

        var result = service.place(new PlaceOrderCommand(
                "customer-1",
                "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "burger-1",
                        "Classic Burger",
                        2,
                        new BigDecimal("5.50")
                ))
        ));

        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);

        var accepted = (PlaceOrderResult.Accepted) result;
        var order = accepted.order();
        var fact = accepted.fact();

        assertThat(order.status()).isEqualTo("PLACED");
        assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-10-07T10:00:00Z"));
        assertThat(order.total()).isEqualByComparingTo("11.00");
        assertThat(repository.size()).isEqualTo(1);
        assertThat(repository.findCurrentById(order.id())).isPresent();

        assertThat(fact.orderId()).isEqualTo(order.id());
        assertThat(fact.customerId()).isEqualTo(order.customerId());
        assertThat(fact.restaurantId()).isEqualTo(order.restaurantId());
        assertThat(fact.occurredAt()).isEqualTo(order.placedAt());
        assertThat(fact.total()).isEqualByComparingTo(order.total());
    }
}
