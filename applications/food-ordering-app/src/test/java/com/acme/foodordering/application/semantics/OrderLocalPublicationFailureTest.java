package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * P11: publication and cross-object fault boundaries remain deliberately
 * outside the domain's immutable successor atomicity.
 */
class OrderLocalPublicationFailureTest {
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    private Order placed() {
        return Order.place(OrderId.random(), new CustomerId("c"), new RestaurantId("r"),
                List.of(new OrderLine("dish", "Dish", 1, BigDecimal.TEN)), AT);
    }

    private static class FaultRepository implements OrderRepository {
        Order current;
        boolean throwBeforeWrite;
        boolean throwAfterWrite;
        int writeAttempts;
        FaultRepository(Order current) { this.current = current; }

        @Override public Optional<Order> findCurrentById(OrderId id) {
            return Optional.ofNullable(current).filter(order -> order.id().equals(id));
        }

        @Override public void saveCurrent(Order successor) {
            writeAttempts++;
            if (throwBeforeWrite) throw new IllegalStateException("injected failure before publication");
            current = successor; // Simulate publication of one coherent reference.
            if (throwAfterWrite) throw new IllegalStateException("injected failure after publication");
        }
    }

    private OrderWorkflowService service(OrderRepository repository) {
        return new OrderWorkflowService(repository, Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test void failureBeforeReferencePublicationLeavesOldOrderCompletelyVisible() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        repo.throwBeforeWrite = true;

        assertThatThrownBy(() -> service(repo).recordCancellation(prior.id()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("before publication");
        assertThat(repo.current).isSameAs(prior);
        assertThat(repo.current.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(repo.current.workflowOccurrences()).isEmpty();
        assertThat(repo.writeAttempts).isEqualTo(1);
    }

    @Test void failureAfterReferencePublicationLeavesNewOrderVisibleDespiteCallerException() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        repo.throwAfterWrite = true;

        assertThatThrownBy(() -> service(repo).recordCancellation(prior.id()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("after publication");
        assertThat(repo.current.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(repo.current.workflowOccurrences()).extracting(x -> x.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(repo.writeAttempts).isEqualTo(1);
        // This proves an error returned to caller is NOT proof that no local effect occurred.
    }

    @Test void successfulReferencePublicationNeverExposesPartialStatusFactPair() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        var snapshot = service(repo).recordCancellation(prior.id());
        assertThat(snapshot.status()).isEqualTo("CANCELLED");
        assertThat(repo.current.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(repo.current.workflowOccurrences()).hasSize(1);
        assertThat(repo.current.workflowOccurrences().get(0).action())
                .isEqualTo(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(prior.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(prior.workflowOccurrences()).isEmpty();
    }

    @Test void twoIndependentObjectsAreNotAtomicallyUpdatedByOrderPublication() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        List<String> independentAudit = new ArrayList<>();
        assertThatThrownBy(() -> {
            service(repo).recordCancellation(prior.id());
            throw new IllegalStateException("audit write failed before second object update");
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("audit write failed");
        assertThat(repo.current.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(repo.current.workflowOccurrences()).hasSize(1);
        assertThat(independentAudit).isEmpty();
        // Deliberate test-only two-object fixture, not a new audit subsystem.
    }

    @Test void twoIndividuallyLegalStaleSuccessorsCanOverwriteEachOther() {
        var paidAndAccepted = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        var repo = new FaultRepository(paidAndAccepted);
        var fromA = paidAndAccepted.recordPreparationStarted(AT);
        var fromB = paidAndAccepted.recordCancellation(AT);

        repo.saveCurrent(fromA);
        assertThat(repo.current.status()).isEqualTo(OrderStatus.PREPARING);
        repo.saveCurrent(fromB); // Last-write-wins using stale predecessor.
        assertThat(repo.current.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(repo.current.workflowOccurrences()).noneMatch(x ->
                x.action() == OrderWorkflowAction.PREPARATION_STARTED);
        // P10 invariants hold for both local snapshots. P11 does not solve races.
    }

    @Test void expectedBusinessDenialNeverAttemptsPublication() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        assertThatThrownBy(() -> service(repo).recordCompletion(prior.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(repo.writeAttempts).isZero();
        assertThat(repo.current).isSameAs(prior);
    }

    @Test void afterPublicationFailureIsNotMisclassifiedAsRoutineBusinessRefusal() {
        var prior = placed();
        var repo = new FaultRepository(prior);
        repo.throwAfterWrite = true;
        assertThatThrownBy(() -> service(repo).execute(prior.id(), OrderWorkflowAction.ORDER_CANCELLED))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("after publication");
        assertThat(repo.current.status()).isEqualTo(OrderStatus.CANCELLED);
    }
}
