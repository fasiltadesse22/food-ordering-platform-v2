package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAtomicityAndConsistencyTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T05:00:00Z");
    private static final CustomerId CUSTOMER =
            new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void normalCompletionPublishesStateAndCompletionFactInOneValidOrderRepresentation() {
        var preparing = preparingOrder();

        var completed = preparing.recordCompletion(
                RESTAURANT,
                NOW.plusSeconds(3)
        );

        assertThat(completed.status())
                .isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        OrderWorkflowAction.PREPARATION_STARTED,
                        OrderWorkflowAction.ORDER_COMPLETED
                );

        OrderInvariants.verify(
                completed.status(),
                completed.workflowOccurrences()
        );

        assertThat(preparing.status())
                .isEqualTo(OrderStatus.PREPARING);
        assertThat(preparing.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .doesNotContain(OrderWorkflowAction.ORDER_COMPLETED);
    }

    @Test
    void statusFirstSplitWriteFailureProducesInvariantInvalidPartialState() {
        var fragile = FragileSplitCompletion.from(preparingOrder());

        fragile.writeCompletedStatus();

        assertThatThrownBy(fragile::failBeforeCompletionFact)
                .isInstanceOf(InjectedFailure.class)
                .hasMessage("failure after status write");

        assertThat(fragile.status)
                .isEqualTo(OrderStatus.COMPLETED);
        assertThat(fragile.history)
                .extracting(OrderWorkflowOccurrence::action)
                .doesNotContain(OrderWorkflowAction.ORDER_COMPLETED);

        assertThatThrownBy(fragile::verify)
                .isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(
                        OrderInvariantViolationException.Code
                                .COMPLETED_STATE_REQUIRES_ACCEPTANCE_PREPARATION_AND_COMPLETION_FACTS
                ));
    }

    @Test
    void historyFirstSplitWriteFailureProducesInvariantInvalidPartialState() {
        var fragile = FragileSplitCompletion.from(preparingOrder());

        fragile.appendCompletionFact(NOW.plusSeconds(3));

        assertThatThrownBy(fragile::failBeforeCompletedStatus)
                .isInstanceOf(InjectedFailure.class)
                .hasMessage("failure after history write");

        assertThat(fragile.status)
                .isEqualTo(OrderStatus.PREPARING);
        assertThat(fragile.history)
                .extracting(OrderWorkflowOccurrence::action)
                .contains(OrderWorkflowAction.ORDER_COMPLETED);

        assertThatThrownBy(fragile::verify)
                .isInstanceOf(OrderInvariantViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderInvariantViolationException) error).code()
                ).isEqualTo(
                        OrderInvariantViolationException.Code
                                .COMPLETION_FACT_REQUIRES_COMPLETED_STATE
                ));
    }

    @Test
    void splitWritesBecomeConsistentOnlyAfterBothRelatedChangesComplete() {
        var fragile = FragileSplitCompletion.from(preparingOrder());

        fragile.writeCompletedStatus();
        fragile.appendCompletionFact(NOW.plusSeconds(3));

        fragile.verify();

        assertThat(fragile.status)
                .isEqualTo(OrderStatus.COMPLETED);
        assertThat(fragile.history)
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED,
                        OrderWorkflowAction.PREPARATION_STARTED,
                        OrderWorkflowAction.ORDER_COMPLETED
                );
    }

    private static Order preparingOrder() {
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

    private static final class FragileSplitCompletion {
        private OrderStatus status;
        private final List<OrderWorkflowOccurrence> history;

        private FragileSplitCompletion(
                OrderStatus status,
                List<OrderWorkflowOccurrence> history
        ) {
            this.status = status;
            this.history = new ArrayList<>(history);
        }

        static FragileSplitCompletion from(Order order) {
            return new FragileSplitCompletion(
                    order.status(),
                    order.workflowOccurrences()
            );
        }

        void writeCompletedStatus() {
            status = OrderStatus.COMPLETED;
        }

        void appendCompletionFact(Instant occurredAt) {
            history.add(new OrderWorkflowOccurrence(
                    OrderWorkflowAction.ORDER_COMPLETED,
                    WorkflowParticipant.RESTAURANT_OPERATOR,
                    occurredAt
            ));
        }

        void failBeforeCompletionFact() {
            throw new InjectedFailure("failure after status write");
        }

        void failBeforeCompletedStatus() {
            throw new InjectedFailure("failure after history write");
        }

        void verify() {
            OrderInvariants.verify(status, history);
        }
    }

    private static final class InjectedFailure
            extends RuntimeException {
        private InjectedFailure(String message) {
            super(message);
        }
    }
}
