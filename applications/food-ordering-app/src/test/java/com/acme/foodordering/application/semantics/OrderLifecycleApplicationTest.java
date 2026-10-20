package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.InvalidOrderTransitionException;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.OrderStatus;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies application-level no-save-on-denial and same-ID current state.
 * This is NOT a test of concurrent atomicity or durable persistence.
 */
class OrderLifecycleApplicationTest {
    private static final Instant NOW = Instant.parse("2026-10-20T11:00:00Z");

    private static final class CountingRepository implements OrderRepository {
        private Order current;
        private int saves;

        CountingRepository(Order order) { this.current = order; }

        @Override public void saveCurrent(Order order) { current = order; saves++; }

        @Override public Optional<Order> findCurrentById(OrderId id) {
            return current.id().equals(id) ? Optional.of(current) : Optional.empty();
        }
    }

    private Order fresh() {
        return Order.place(OrderId.random(), new CustomerId("customer-1"),
                new RestaurantId("restaurant-1"),
                List.of(new OrderLine("burger", "Burger", 1, new BigDecimal("10.00"))), NOW);
    }

    private OrderWorkflowService service(CountingRepository repository) {
        return new OrderWorkflowService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void invalidPreparationIsRejectedBeforeRepositorySave() {
        var initial = fresh();
        var repository = new CountingRepository(initial);
        var workflow = service(repository);

        assertThatThrownBy(() -> workflow.recordPreparationStarted(initial.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(repository.saves).isZero();
        assertThat(repository.current).isSameAs(initial);
        assertThat(repository.current.workflowOccurrences()).isEmpty();
    }

    @Test
    void legalTransitionPublishesExactlyOneSuccessorWithSameIdentity() {
        var initial = fresh();
        var repository = new CountingRepository(initial);
        var workflow = service(repository);

        var accepted = workflow.recordRestaurantAcceptance(initial.id());

        assertThat(accepted.status()).isEqualTo("ACCEPTED");
        assertThat(accepted.id()).isEqualTo(initial.id());
        assertThat(repository.saves).isEqualTo(1);
        assertThat(repository.current).isNotSameAs(initial);
        assertThat(repository.current.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(repository.current.workflowOccurrences())
                .extracting(o -> o.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(initial.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void terminalStateRejectsLaterAcceptanceWithoutNewOccurrenceOrSave() {
        var initial = fresh().recordCancellation(NOW);
        var repository = new CountingRepository(initial);
        var workflow = service(repository);

        assertThatThrownBy(() -> workflow.recordRestaurantAcceptance(initial.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(repository.saves).isZero();
        assertThat(repository.current).isSameAs(initial);
        assertThat(repository.current.workflowOccurrences()).hasSize(1);
    }

    @Test
    void rejectedOrderAllowsRefundRequestWithoutReopeningFulfillment() {
        var initial = fresh().recordPayment(NOW).recordRestaurantRejection(NOW);
        var repository = new CountingRepository(initial);
        var workflow = service(repository);

        var returned = workflow.recordRefundRequest(initial.id());
        assertThat(returned.status()).isEqualTo("REJECTED");
        assertThat(returned.workflow())
                .extracting(o -> o.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.RESTAURANT_REJECTED,
                        OrderWorkflowAction.REFUND_REQUESTED);
        assertThat(repository.saves).isEqualTo(1);
        assertThat(repository.current.id()).isEqualTo(initial.id());
    }
}
