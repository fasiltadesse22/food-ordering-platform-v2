package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.in.OrderWorkflowResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.*;
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

class OrderWorkflowResultClassificationTest {
    private static final Instant AT = Instant.parse("2026-10-24T10:00:00Z");

    private Order order() {
        return Order.place(OrderId.random(), new CustomerId("c"), new RestaurantId("r"),
            List.of(new OrderLine("dish", "Dish", 1, BigDecimal.TEN)), AT);
    }

    private static final class StubRepository implements OrderRepository {
        Order current;
        int writes;
        StubRepository(Order order) { current = order; }
        @Override public void saveCurrent(Order order) { current = order; writes++; }
        @Override public Optional<Order> findCurrentById(OrderId id) {
            return Optional.ofNullable(current).filter(o -> o.id().equals(id));
        }
    }

    private OrderWorkflowService workflow(OrderRepository repository) {
        return new OrderWorkflowService(repository, Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test void illegalTransitionIsTypedBusinessResultAndNeverSaved() {
        var original = order();
        var repo = new StubRepository(original);
        var result = workflow(repo).execute(original.id(), OrderWorkflowAction.ORDER_COMPLETED);
        assertThat(result).isInstanceOf(OrderWorkflowResult.Rejected.class);
        var rejected = (OrderWorkflowResult.Rejected) result;
        assertThat(rejected.rejection().code())
            .isEqualTo(OrderWorkflowResult.BusinessRejection.Code.ILLEGAL_LIFECYCLE_TRANSITION);
        assertThat(repo.writes).isZero();
        assertThat(repo.current).isSameAs(original);
    }

    @Test void restaurantRejectionIsAcceptedBusinessActionNotTechnicalFailure() {
        var original = order();
        var repo = new StubRepository(original);
        var result = workflow(repo).execute(original.id(), OrderWorkflowAction.RESTAURANT_REJECTED);
        assertThat(result).isInstanceOf(OrderWorkflowResult.Accepted.class);
        assertThat(((OrderWorkflowResult.Accepted)result).order().status()).isEqualTo("REJECTED");
        assertThat(repo.writes).isEqualTo(1);
    }

    @Test void unknownOrderIsNotMisclassifiedAsLifecycleRejection() {
        var repo = new StubRepository(null);
        var result = order();
        assertThatThrownBy(() -> workflow(repo).execute(result.id(), OrderWorkflowAction.ORDER_CANCELLED))
            .isInstanceOf(com.acme.foodordering.application.service.OrderNotFoundException.class);
    }

    @Test void unexpectedRepositoryFailurePropagatesInsteadOfBecomingBusinessDenial() {
        var original = order();
        OrderRepository broken = new OrderRepository() {
            @Override public void saveCurrent(Order order) {
                throw new IllegalStateException("storage malfunction");
            }
            @Override public Optional<Order> findCurrentById(OrderId id) {
                return Optional.of(original);
            }
        };
        assertThatThrownBy(() -> workflow(broken).execute(original.id(), OrderWorkflowAction.RESTAURANT_ACCEPTED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("storage malfunction");
    }

    @Test void noBusinessResultReturnedAfterRepositoryWriteFailure() {
        var original = order();
        OrderRepository broken = new OrderRepository() {
            @Override public void saveCurrent(Order order) {
                throw new IllegalStateException("write failed");
            }
            @Override public Optional<Order> findCurrentById(OrderId id) {
                return Optional.of(original);
            }
        };
        assertThatThrownBy(() -> workflow(broken).execute(original.id(), OrderWorkflowAction.RESTAURANT_REJECTED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("write failed");
    }
}
