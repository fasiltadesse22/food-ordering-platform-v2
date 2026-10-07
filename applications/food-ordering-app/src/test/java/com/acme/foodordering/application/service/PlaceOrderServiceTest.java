package com.acme.foodordering.application.service;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderServiceTest {

    @Test
    void persistsThePlacedOrderThroughTheOutputPort() {
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

        assertThat(result.status()).isEqualTo("PLACED");
        assertThat(result.placedAt()).isEqualTo(Instant.parse("2026-10-07T10:00:00Z"));
        assertThat(result.total()).isEqualByComparingTo("11.00");
        assertThat(repository.size()).isEqualTo(1);
        assertThat(repository.findById(com.acme.foodordering.domain.order.OrderId.from(result.id())))
                .isPresent();
    }
}
