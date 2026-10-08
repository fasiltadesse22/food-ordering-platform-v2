package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
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

class OrderActionFailureSemanticsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-08T11:00:00Z"),
            ZoneOffset.UTC
    );
    private static final CustomerId CUSTOMER = new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-1");

    @Test
    void illegalLifecycleRequestIsARejectedBusinessOutcomeNotApplicationFailure() {
        var repository = new InMemoryOrderRepository();
        var order = placedOrder();
        repository.saveCurrent(order);
        var service = new OrderWorkflowService(repository, CLOCK);

        var result = service.recordPreparationStarted(order.id(), RESTAURANT);

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(OrderActionRejection.Code.ILLEGAL_TRANSITION);
        assertThat(repository.findCurrentById(order.id())).containsSame(order);
    }

    @Test
    void ownershipViolationIsARejectedBusinessOutcomeNotApplicationFailure() {
        var repository = new InMemoryOrderRepository();
        var order = placedOrder();
        repository.saveCurrent(order);
        var service = new OrderWorkflowService(repository, CLOCK);

        var result = service.recordCancellation(
                order.id(),
                new CustomerId("customer-2")
        );

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(
                        OrderActionRejection.Code.CUSTOMER_DOES_NOT_OWN_ORDER
                );
    }

    @Test
    void unknownOrderIsAnExpectedRejectedOutcome() {
        var service = new OrderWorkflowService(
                new InMemoryOrderRepository(),
                CLOCK
        );

        var result = service.recordCancellation(
                OrderId.from("123e4567-e89b-12d3-a456-426614174099"),
                CUSTOMER
        );

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(OrderActionRejection.Code.ORDER_NOT_FOUND);
    }

    @Test
    void repositoryReadFailureRemainsATechnicalFailureAndIsNotConvertedToRejection() {
        var service = new OrderWorkflowService(
                new ReadFailingRepository(),
                CLOCK
        );

        assertThatThrownBy(() -> service.recordCancellation(
                OrderId.from("123e4567-e89b-12d3-a456-426614174000"),
                CUSTOMER
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("repository unavailable");
    }

    @Test
    void repositoryWriteFailureRemainsATechnicalFailureAndIsNotConvertedToRejection() {
        var order = placedOrder();
        var service = new OrderWorkflowService(
                new WriteFailingRepository(order),
                CLOCK
        );

        assertThatThrownBy(() -> service.recordCancellation(
                order.id(),
                CUSTOMER
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("repository write failed");
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
                CLOCK.instant()
        );
    }

    private static final class ReadFailingRepository implements OrderRepository {
        @Override
        public void saveCurrent(Order order) {
            throw new AssertionError("save should not be reached");
        }

        @Override
        public Optional<Order> findCurrentById(OrderId orderId) {
            throw new IllegalStateException("repository unavailable");
        }
    }

    private static final class WriteFailingRepository implements OrderRepository {
        private final Order current;

        private WriteFailingRepository(Order current) {
            this.current = current;
        }

        @Override
        public void saveCurrent(Order order) {
            throw new IllegalStateException("repository write failed");
        }

        @Override
        public Optional<Order> findCurrentById(OrderId orderId) {
            return Optional.of(current);
        }
    }
}
