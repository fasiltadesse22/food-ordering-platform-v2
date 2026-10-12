package com.acme.foodordering.domain.order;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class OrderEntityIdentityEqualityTest {
    private static final Instant AT = Instant.parse("2026-10-12T09:00:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-a");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-a");

    private static Order order(String rawId) {
        return Order.place(
            OrderId.from(rawId), CUSTOMER, RESTAURANT,
            List.of(new OrderLine("burger", "Burger", 2, new BigDecimal("5.50"))), AT);
    }

    @Test void sameDomainIdentityDoesNotImplySameJavaObjectOrOrderEquals() {
        var initial = order("123e4567-e89b-12d3-a456-426614174081");
        var accepted = initial.recordRestaurantAcceptance(RESTAURANT, AT.plusSeconds(1));
        assertThat(initial).isNotSameAs(accepted);
        assertThat(initial.id()).isEqualTo(accepted.id());
        assertThat(initial.equals(accepted)).isFalse();
        assertThat(initial.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test void distinctOrdersWithEquivalentAttributesRemainIndependentlyAddressable() {
        var first = order("123e4567-e89b-12d3-a456-426614174081");
        var second = order("123e4567-e89b-12d3-a456-426614174082");
        var repo = new InMemoryOrderRepository();
        repo.saveCurrent(first);
        repo.saveCurrent(second);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.status()).isEqualTo(second.status());
        assertThat(first.lines()).isEqualTo(second.lines());
        assertThat(repo.size()).isEqualTo(2);
    }

    @Test void hashSetUsingOrderReferenceEqualityDiffersFromBusinessIdentitySet() {
        var original = order("123e4567-e89b-12d3-a456-426614174081");
        var successor = original.recordRestaurantAcceptance(RESTAURANT, AT.plusSeconds(1));
        var objects = new HashSet<Order>();
        objects.add(original);
        objects.add(successor);
        var identities = new HashSet<OrderId>();
        identities.add(original.id());
        identities.add(successor.id());
        assertThat(objects).hasSize(2);
        assertThat(identities).hasSize(1);
    }

    @Test void differentIdentifierWrapperKindsAreNotInterchangeable() {
        assertThat(new CustomerId("same-text"))
            .isNotEqualTo(new RestaurantId("same-text"));
        assertThat(OrderId.from("123e4567-e89b-12d3-a456-426614174081"))
            .isEqualTo(OrderId.from("123e4567-e89b-12d3-a456-426614174081"));
    }

    @Test void terminalLifecycleRetainsOrderIdentityAndDoesNotMutateEarlierRepresentations() {
        var placed = order("123e4567-e89b-12d3-a456-426614174081");
        var accepted = placed.recordRestaurantAcceptance(RESTAURANT, AT.plusSeconds(1));
        var preparing = accepted.recordPreparationStarted(RESTAURANT, AT.plusSeconds(2));
        var completed = preparing.recordCompletion(RESTAURANT, AT.plusSeconds(3));
        assertThat(completed.id()).isEqualTo(placed.id());
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(preparing.status()).isEqualTo(OrderStatus.PREPARING);
    }
}
