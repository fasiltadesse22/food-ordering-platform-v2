package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.OrderStatus;
import com.acme.foodordering.domain.order.RestaurantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLocalCommitBoundaryTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T05:30:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(10), ZoneOffset.UTC);
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void failureBeforeRepositoryReplacementLeavesOldAuthorityUnchanged() {
        var preparing = preparingOrder();
        var repository =
                new FailBeforeReplaceRepository(preparing);
        var service = new OrderWorkflowService(
                repository,
                CLOCK
        );

        assertThatThrownBy(() -> service.recordCompletion(
                preparing.id(),
                RESTAURANT
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected failure before authority replacement");

        var authoritative =
                repository.findCurrentById(preparing.id())
                        .orElseThrow();

        assertThat(authoritative)
                .isSameAs(preparing);
        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void successfulRepositoryReplacementPublishesOnlyCompleteNewRepresentation() {
        var preparing = preparingOrder();
        var repository =
                new RecordingRepository(preparing);
        var service = new OrderWorkflowService(
                repository,
                CLOCK
        );

        var result = service.recordCompletion(
                preparing.id(),
                RESTAURANT
        );

        assertThat(result)
                .isInstanceOf(OrderActionResult.Accepted.class);

        var authoritative =
                repository.findCurrentById(preparing.id())
                        .orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.COMPLETED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(o -> o.action().name())
                .contains("ORDER_COMPLETED");
    }

    private static Order preparingOrder() {
        return Order.place(
                OrderId.from("123e4567-e89b-12d3-a456-426614174010"),
                new CustomerId("customer-1"),
                RESTAURANT,
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                )),
                NOW
        )
                .recordRestaurantAcceptance(
                        RESTAURANT,
                        NOW.plusSeconds(1)
                )
                .recordPreparationStarted(
                        RESTAURANT,
                        NOW.plusSeconds(2)
                );
    }

    private static final class FailBeforeReplaceRepository
            implements OrderRepository {

        private final Order current;

        private FailBeforeReplaceRepository(Order current) {
            this.current = current;
        }

        @Override
        public void saveCurrent(Order order) {
            throw new IllegalStateException(
                    "injected failure before authority replacement"
            );
        }

        @Override
        public Optional<Order> findCurrentById(
                OrderId orderId
        ) {
            return Optional.of(current);
        }
    }

    private static final class RecordingRepository
            implements OrderRepository {

        private Order current;

        private RecordingRepository(Order current) {
            this.current = current;
        }

        @Override
        public void saveCurrent(Order order) {
            current = order;
        }

        @Override
        public Optional<Order> findCurrentById(
                OrderId orderId
        ) {
            return Optional.of(current);
        }
    }
}
