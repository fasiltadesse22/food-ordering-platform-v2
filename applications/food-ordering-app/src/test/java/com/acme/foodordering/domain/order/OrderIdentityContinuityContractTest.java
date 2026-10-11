package com.acme.foodordering.domain.order;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderIdentityContinuityContractTest {

    private static final Instant PLACED_AT = Instant.parse("2026-10-11T09:00:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-identity-a");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-identity-a");
    private static final String FIRST_ID = "123e4567-e89b-12d3-a456-426614174061";
    private static final String SECOND_ID = "123e4567-e89b-12d3-a456-426614174062";

    @Test
    void normalImmutableLifecycleCreatesDifferentObjectsForOneContinuingOrderIdentity() {
        var original = placed(OrderId.from(FIRST_ID));
        var accepted = original.recordRestaurantAcceptance(
                RESTAURANT, PLACED_AT.plusSeconds(10)
        );
        var preparing = accepted.recordPreparationStarted(
                RESTAURANT, PLACED_AT.plusSeconds(20)
        );
        var completed = preparing.recordCompletion(
                RESTAURANT, PLACED_AT.plusSeconds(30)
        );

        assertThat(original).isNotSameAs(accepted);
        assertThat(accepted).isNotSameAs(preparing);
        assertThat(preparing).isNotSameAs(completed);

        assertThat(completed.id()).isEqualTo(original.id());
        assertThat(completed.customerId()).isEqualTo(original.customerId());
        assertThat(completed.restaurantId()).isEqualTo(original.restaurantId());
        assertThat(completed.placedAt()).isEqualTo(original.placedAt());
        assertThat(completed.lines()).isEqualTo(original.lines());

        assertThat(original.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(original.workflowOccurrences()).isEmpty();
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        OrderWorkflowAction.PREPARATION_STARTED,
                        OrderWorkflowAction.ORDER_COMPLETED
                );
    }

    @Test
    void differentBusinessIdsRemainDistinctEvenWithIdenticalVisibleState() {
        var first = placed(OrderId.from(FIRST_ID));
        var second = placed(OrderId.from(SECOND_ID));
        var repository = new InMemoryOrderRepository();

        assertThat(first.status()).isEqualTo(second.status());
        assertThat(first.customerId()).isEqualTo(second.customerId());
        assertThat(first.restaurantId()).isEqualTo(second.restaurantId());
        assertThat(first.lines()).isEqualTo(second.lines());
        assertThat(first.placedAt()).isEqualTo(second.placedAt());
        assertThat(first.id()).isNotEqualTo(second.id());

        repository.saveCurrent(first);
        repository.saveCurrent(second);

        assertThat(repository.size()).isEqualTo(2);
        assertThat(repository.findCurrentById(first.id())).containsSame(first);
        assertThat(repository.findCurrentById(second.id())).containsSame(second);
    }

    @Test
    void typedIdentityValuesUseValueEqualityButDoNotConflateEntityKinds() {
        var firstId = OrderId.from(FIRST_ID);
        var sameId = OrderId.from(FIRST_ID);

        assertThat(firstId).isNotSameAs(sameId);
        assertThat(firstId).isEqualTo(sameId);
        assertThat(firstId.hashCode()).isEqualTo(sameId.hashCode());
        assertThat(OrderId.from(firstId.toString())).isEqualTo(firstId);

        assertThat(new CustomerId("shared-value"))
                .isNotEqualTo(new RestaurantId("shared-value"));
    }

    @Test
    void independentRepositoryInstancesHaveNoSharedAuthorityEvenWithEqualOrderId() {
        var processARepository = new InMemoryOrderRepository();
        var processBRepository = new InMemoryOrderRepository();
        var identity = OrderId.from(FIRST_ID);
        var order = placed(identity);

        processARepository.saveCurrent(order);

        assertThat(processARepository.findCurrentById(OrderId.from(FIRST_ID)))
                .containsSame(order);
        assertThat(processBRepository.findCurrentById(OrderId.from(FIRST_ID)))
                .isEmpty();

        // This models two independent in-memory repository instances.
        // It is not a substitute for the real JVM replacement experiment in C1.1-P18.
    }

    @Test
    void currentAuthorityCanAdvanceWhileEarlierSameIdentityRepresentationRemainsLocallyUsable() {
        var repository = new InMemoryOrderRepository();
        var original = placed(OrderId.from(FIRST_ID));
        repository.saveCurrent(original);

        var accepted = original.recordRestaurantAcceptance(
                RESTAURANT, PLACED_AT.plusSeconds(10)
        );
        repository.saveCurrent(accepted);

        var staleLocallyLegalCancellation = original.recordCancellation(
                CUSTOMER, PLACED_AT.plusSeconds(20)
        );

        assertThat(staleLocallyLegalCancellation.id()).isEqualTo(accepted.id());
        assertThat(staleLocallyLegalCancellation.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(repository.findCurrentById(original.id()).orElseThrow())
                .isSameAs(accepted);
        assertThat(repository.findCurrentById(original.id()).orElseThrow().status())
                .isEqualTo(OrderStatus.ACCEPTED);

        // The candidate was deliberately NOT saved here. C1.1-P17 already
        // reproduces the actual stale replacement failure. Identity equality
        // does not establish write permission, freshness, or authoritative priority.
    }

    private static Order placed(OrderId id) {
        return Order.place(
                id,
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine(
                        "burger-identity-a",
                        "Classic Burger",
                        2,
                        new BigDecimal("5.50")
                )),
                PLACED_AT
        );
    }
}
