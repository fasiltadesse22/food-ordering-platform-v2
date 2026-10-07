package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.service.GetOrderService;
import com.acme.foodordering.application.service.OrderNotFoundException;
import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.RestaurantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdentityAndAuthorityTest {

    private static final String ORDER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final Instant PLACED_AT = Instant.parse("2026-10-07T10:00:00Z");

    @Test
    void equalIdentifierValuesAddressTheSameLogicalRepositoryEntry() {
        var repository = new InMemoryOrderRepository();
        var firstIdentifierObject = OrderId.from(ORDER_ID);
        var secondIdentifierObject = OrderId.from(ORDER_ID);

        assertThat(firstIdentifierObject)
                .isEqualTo(secondIdentifierObject)
                .isNotSameAs(secondIdentifierObject);

        var order = order(firstIdentifierObject, 1);
        repository.saveCurrent(order);

        var loaded = repository.findCurrentById(secondIdentifierObject);

        assertThat(loaded).containsSame(order);
    }

    @Test
    void differentJavaObjectsCanRepresentTheSameBusinessIdentityAndRepositorySelectsCurrentState() {
        var repository = new InMemoryOrderRepository();
        var identity = OrderId.from(ORDER_ID);

        var firstRepresentation = order(identity, 1);
        var sameIdentityDifferentObject = order(OrderId.from(ORDER_ID), 2);

        assertThat(firstRepresentation).isNotSameAs(sameIdentityDifferentObject);
        assertThat(firstRepresentation.id()).isEqualTo(sameIdentityDifferentObject.id());

        repository.saveCurrent(firstRepresentation);
        repository.saveCurrent(sameIdentityDifferentObject);

        var current = repository.findCurrentById(identity).orElseThrow();

        assertThat(repository.size()).isEqualTo(1);
        assertThat(current).isSameAs(sameIdentityDifferentObject);
        assertThat(current.id()).isEqualTo(firstRepresentation.id());
    }

    @Test
    void detachedSnapshotCanBecomeStaleRelativeToRepositoryCurrentState() {
        var repository = new InMemoryOrderRepository();
        var identity = OrderId.from(ORDER_ID);

        var firstRepresentation = order(identity, 1);
        repository.saveCurrent(firstRepresentation);

        var earlierSnapshot = OrderSnapshot.from(
                repository.findCurrentById(identity).orElseThrow()
        );

        var laterCurrentRepresentation = order(OrderId.from(ORDER_ID), 2);
        repository.saveCurrent(laterCurrentRepresentation);

        var current = repository.findCurrentById(identity).orElseThrow();

        assertThat(earlierSnapshot.id()).isEqualTo(current.id());
        assertThat(earlierSnapshot.total()).isEqualByComparingTo("5.50");
        assertThat(current.total()).isEqualByComparingTo("11.00");

        // Same business identity, different observations in time.
        // The snapshot did not magically update when repository current state changed.
    }

    @Test
    void unknownIdentityHasNoCurrentAuthoritativeOrder() {
        var repository = new InMemoryOrderRepository();
        var service = new GetOrderService(repository);
        var unknown = OrderId.from(ORDER_ID);

        assertThat(repository.findCurrentById(unknown)).isEmpty();

        assertThatThrownBy(() -> service.get(unknown))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(ORDER_ID);
    }

    private static Order order(OrderId id, int quantity) {
        return Order.place(
                id,
                new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        quantity,
                        new BigDecimal("5.50")
                )),
                PLACED_AT
        );
    }
}
