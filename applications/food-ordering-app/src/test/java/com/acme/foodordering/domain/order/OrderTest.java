package com.acme.foodordering.domain.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void placesANonEmptyOrder() {
        var order = Order.place(
                OrderId.random(),
                new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine("burger-1", "Classic Burger", 2, new BigDecimal("5.50"))),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.total()).isEqualByComparingTo("11.00");
        assertThat(order.lines()).hasSize(1);
    }

    @Test
    void rejectsAnEmptyOrder() {
        assertThatThrownBy(() -> Order.place(
                OrderId.random(),
                new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(),
                Instant.parse("2026-10-07T10:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("an order must contain at least one line");
    }
}
