package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class OrderLocalAtomicAssemblyTest {
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    private Order placed() {
        return Order.place(
            OrderId.random(), new CustomerId("customer"), new RestaurantId("restaurant"),
            List.of(new OrderLine("dish", "Dish", 2, new BigDecimal("5.50"))), AT
        );
    }

    private Order failAt(Order source, Order.AssemblyStage stage,
                         OrderWorkflowAction action, WorkflowParticipant participant) {
        assertThatThrownBy(() -> source.assembleTransition(action, participant, AT,
                actual -> { if (actual == stage) throw new IllegalStateException("injected " + stage); }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("injected " + stage);
        return source;
    }

    private void assertOriginalUnmodified(Order source, OrderStatus status, int history) {
        assertThat(source.status()).isEqualTo(status);
        assertThat(source.workflowOccurrences()).hasSize(history);
        assertThat(source.total()).isEqualByComparingTo("11.00");
    }

    @Test void throwAfterTargetDecisionCannotPublishCancelledWithoutOccurrence() {
        var source = placed();
        failAt(source, Order.AssemblyStage.TARGET_DECIDED,
               OrderWorkflowAction.ORDER_CANCELLED, WorkflowParticipant.CUSTOMER);
        assertOriginalUnmodified(source, OrderStatus.PLACED, 0);
    }

    @Test void throwAfterFactIsStagedCannotExposeOccurrenceWithoutStatus() {
        var source = placed();
        failAt(source, Order.AssemblyStage.FACT_STAGED,
               OrderWorkflowAction.ORDER_CANCELLED, WorkflowParticipant.CUSTOMER);
        assertOriginalUnmodified(source, OrderStatus.PLACED, 0);
    }

    @Test void throwAfterNewSuccessorConstructionStillCannotMutateOriginal() {
        var source = placed();
        failAt(source, Order.AssemblyStage.SUCCESSOR_CONSTRUCTED,
               OrderWorkflowAction.ORDER_CANCELLED, WorkflowParticipant.CUSTOMER);
        assertOriginalUnmodified(source, OrderStatus.PLACED, 0);
    }

    @Test void successfulCancellationReturnsStatusAndFactTogether() {
        var original = placed();
        var successor = original.recordCancellation(AT);
        assertOriginalUnmodified(original, OrderStatus.PLACED, 0);
        assertThat(successor).isNotSameAs(original);
        assertThat(successor.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(successor.workflowOccurrences()).hasSize(1);
        assertThat(successor.workflowOccurrences().get(0).action())
            .isEqualTo(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(successor.id()).isEqualTo(original.id());
    }

    @Test void acceptanceReturnsOneNewFactWithTargetStatus() {
        var original = placed();
        var successor = original.recordRestaurantAcceptance(AT);
        assertThat(successor.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(successor.workflowOccurrences()).hasSize(1);
        assertThat(successor.workflowOccurrences().get(0).action())
            .isEqualTo(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertOriginalUnmodified(original, OrderStatus.PLACED, 0);
    }

    @Test void localPaymentMilestoneChangesHistoryButNotFulfillmentStatus() {
        var original = placed();
        var successor = original.recordPayment(AT);
        assertThat(successor.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(successor.workflowOccurrences()).hasSize(1);
        assertThat(successor.workflowOccurrences().get(0).action())
            .isEqualTo(OrderWorkflowAction.PAYMENT_RECORDED);
        assertOriginalUnmodified(original, OrderStatus.PLACED, 0);
    }

    @Test void guardRejectsBeforeAnyAssemblyProbeRuns() {
        var original = placed();
        var seen = new AtomicInteger(0);
        assertThatThrownBy(() -> original.assembleTransition(
                OrderWorkflowAction.ORDER_COMPLETED, WorkflowParticipant.RESTAURANT_OPERATOR, AT,
                stage -> seen.incrementAndGet()))
            .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(seen.get()).isZero();
        assertOriginalUnmodified(original, OrderStatus.PLACED, 0);
    }

    @Test void successfulTransitionVisitsAllThreeInternalAssemblyStagesInOrder() {
        var original = placed();
        List<Order.AssemblyStage> stages = new ArrayList<>();
        var next = original.assembleTransition(
            OrderWorkflowAction.ORDER_CANCELLED, WorkflowParticipant.CUSTOMER, AT, stages::add);
        assertThat(stages).containsExactly(
            Order.AssemblyStage.TARGET_DECIDED,
            Order.AssemblyStage.FACT_STAGED,
            Order.AssemblyStage.SUCCESSOR_CONSTRUCTED);
        assertThat(next.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(next.workflowOccurrences()).hasSize(1);
    }

    @Test void originalAndSuccessorExposeImmutableHistory() {
        var original = placed();
        var next = original.recordCancellation(AT);
        assertThatThrownBy(() -> next.workflowOccurrences().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> next.lines().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(original.workflowOccurrences()).isEmpty();
        assertThat(next.workflowOccurrences()).hasSize(1);
    }

    @Test void deliberatelyUnsafeMutableSplitMakesPartialStateObservable() {
        class UnsafeMutableOrder {
            OrderStatus status = OrderStatus.PLACED;
            final List<OrderWorkflowOccurrence> events = new ArrayList<>();
            void cancelWithFailure() {
                status = OrderStatus.CANCELLED;  // wrongly published before fact
                throw new IllegalStateException("failure before occurrence append");
            }
        }
        var unsafe = new UnsafeMutableOrder();
        assertThatThrownBy(unsafe::cancelWithFailure).isInstanceOf(IllegalStateException.class);
        assertThat(unsafe.status).isEqualTo(OrderStatus.CANCELLED);
        assertThat(unsafe.events).isEmpty(); // violates status/fact coupling
        // This deliberately broken fixture is NOT the production Order implementation.
    }

    @Test void paymentAndCancellationPreserveBothEarlierAndNewFacts() {
        var original = placed().recordPayment(AT);
        var after = original.recordCancellation(AT);
        assertThat(original.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(original.workflowOccurrences()).hasSize(1);
        assertThat(after.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(after.workflowOccurrences()).extracting(OrderWorkflowOccurrence::action)
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED, OrderWorkflowAction.ORDER_CANCELLED);
    }
}
