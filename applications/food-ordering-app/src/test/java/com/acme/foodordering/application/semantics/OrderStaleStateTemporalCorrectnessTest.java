package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderGuardViolationException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStaleStateTemporalCorrectnessTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T10:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC);
    private static final CustomerId CUSTOMER =
            new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void modificationBasedOnPrePaymentSnapshotCanOverwriteLaterPaymentFact() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var staleEditableSnapshot =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(staleEditableSnapshot.status())
                .isEqualTo(OrderStatus.PLACED);
        assertThat(staleEditableSnapshot.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(OrderWorkflowAction.PAYMENT_RECORDED);

        assertAccepted(service.recordPayment(placed.id()));

        var afterPayment =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(afterPayment.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);

        var staleModification = staleEditableSnapshot.modifyLines(
                CUSTOMER,
                replacementLines(),
                NOW.plusSeconds(60)
        );

        repository.saveCurrent(staleModification);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.PLACED);
        assertThat(authoritative.lines())
                .isEqualTo(replacementLines());
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(OrderWorkflowAction.PAYMENT_RECORDED);
    }

    @Test
    void paymentDecisionBasedOnPreCancellationSnapshotCanResurrectPlacedAndEraseCancellation() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var stalePayableSnapshot =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertAccepted(service.recordCancellation(
                placed.id(),
                CUSTOMER
        ));

        var afterCancellation =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(afterCancellation.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(afterCancellation.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);

        var stalePayment =
                stalePayableSnapshot.recordPayment(NOW.plusSeconds(60));

        repository.saveCurrent(stalePayment);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.PLACED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test
    void freshPaymentAfterCancellationIsCurrentlyAllowedWithoutResurrectingPlaced() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        assertAccepted(service.recordCancellation(
                placed.id(),
                CUSTOMER
        ));
        assertAccepted(service.recordPayment(placed.id()));

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.PAYMENT_RECORDED
                );
    }

    @Test
    void guardCanRejectFreshAcceptedOrderWhileSameModificationPassesAgainstStalePlacedCopy() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var stalePlacedCopyA =
                repository.findCurrentById(placed.id()).orElseThrow();
        var stalePlacedCopyB =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(stalePlacedCopyA).isSameAs(stalePlacedCopyB);

        assertAccepted(service.recordRestaurantAcceptance(
                placed.id(),
                RESTAURANT
        ));

        var authoritativeAccepted =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritativeAccepted.status())
                .isEqualTo(OrderStatus.ACCEPTED);

        assertThatThrownBy(() -> authoritativeAccepted.modifyLines(
                CUSTOMER,
                replacementLines(),
                NOW.plusSeconds(60)
        ))
                .isInstanceOf(OrderGuardViolationException.class)
                .satisfies(error -> assertThat(
                        ((OrderGuardViolationException) error).code()
                ).isEqualTo(
                        OrderGuardViolationException.Code
                                .MODIFICATION_REQUIRES_PLACED_ORDER
                ));

        var staleModification = stalePlacedCopyB.modifyLines(
                CUSTOMER,
                replacementLines(),
                NOW.plusSeconds(60)
        );

        assertThat(staleModification.status())
                .isEqualTo(OrderStatus.PLACED);

        repository.saveCurrent(staleModification);

        var authoritativeAfterStaleSave =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritativeAfterStaleSave.status())
                .isEqualTo(OrderStatus.PLACED);
        assertThat(authoritativeAfterStaleSave.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED);
        assertThat(authoritativeAfterStaleSave.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
    }

    private static void assertAccepted(OrderActionResult result) {
        assertThat(result)
                .isInstanceOf(OrderActionResult.Accepted.class);
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from(
                        "123e4567-e89b-12d3-a456-426614174017"
                ),
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                )),
                NOW
        );
    }

    private static List<OrderLine> replacementLines() {
        return List.of(new OrderLine(
                "burger-2",
                "Double Burger",
                1,
                new BigDecimal("7.50")
        ));
    }
}
