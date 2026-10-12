package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.ActorContext;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.CustomerId;
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

import static org.assertj.core.api.Assertions.assertThat;

class OrderAggregateLifecycleReferenceBoundaryTest {
    private static final Instant AT = Instant.parse("2026-10-12T10:20:00Z");
    private static final CustomerId CUSTOMER = new CustomerId("customer-boundary");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-boundary");
    private static final Clock CLOCK = Clock.fixed(AT.plusSeconds(12), ZoneOffset.UTC);

    private static Order newOrder(String rawId) {
        return Order.place(
                OrderId.from(rawId),
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine("burger-1", "Burger", 2, new BigDecimal("5.50"))),
                AT
        );
    }

    @Test
    void twoOrdersSharingCustomerAndRestaurantReferencesHaveIndependentOrderLifecycles() {
        var repository = new InMemoryOrderRepository();
        var first = newOrder("123e4567-e89b-12d3-a456-426614174181");
        var second = newOrder("123e4567-e89b-12d3-a456-426614174182");
        repository.saveCurrent(first);
        repository.saveCurrent(second);

        var accepted = first.recordRestaurantAcceptance(RESTAURANT, AT.plusSeconds(1));
        repository.saveCurrent(accepted);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(accepted.customerId()).isEqualTo(second.customerId());
        assertThat(accepted.restaurantId()).isEqualTo(second.restaurantId());
        assertThat(repository.findCurrentById(first.id()).orElseThrow().status())
                .isEqualTo(OrderStatus.ACCEPTED);
        assertThat(repository.findCurrentById(second.id()).orElseThrow().status())
                .isEqualTo(OrderStatus.PLACED);
        assertThat(repository.size()).isEqualTo(2);
        // Shared references do not merge Order aggregate identities or lifecycles.
    }

    @Test
    void identityReferencesSurviveNormalOrderLifecycleWithoutOwningTheirExternalEntities() {
        var placed = newOrder("123e4567-e89b-12d3-a456-426614174181");
        var accepted = placed.recordRestaurantAcceptance(RESTAURANT, AT.plusSeconds(1));
        var preparing = accepted.recordPreparationStarted(RESTAURANT, AT.plusSeconds(2));
        var completed = preparing.recordCompletion(RESTAURANT, AT.plusSeconds(3));

        for (var state : List.of(placed, accepted, preparing, completed)) {
            assertThat(state.id()).isEqualTo(placed.id());
            assertThat(state.customerId()).isEqualTo(CUSTOMER);
            assertThat(state.restaurantId()).isEqualTo(RESTAURANT);
            assertThat(state.placedAt()).isEqualTo(AT);
        }
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        // This verifies stored identity references, not Customer/Restaurant existence.
    }

    @Test
    void applicationServiceLoadsInvokesOrderRootSavesAndProjectsResult() {
        var repository = new InMemoryOrderRepository();
        var placed = newOrder("123e4567-e89b-12d3-a456-426614174181");
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var result = service.recordRestaurantAcceptance(placed.id(), RESTAURANT);

        assertThat(result).isInstanceOf(OrderActionResult.Accepted.class);
        var snapshot = ((OrderActionResult.Accepted) result).order();
        var current = repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(snapshot.id()).isEqualTo(placed.id());
        assertThat(snapshot.restaurantId()).isEqualTo(RESTAURANT);
        assertThat(snapshot.status()).isEqualTo(OrderStatus.ACCEPTED.name());
        assertThat(current.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(current.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        // The application orchestrates authority publication; Order enforces local rule.
    }

    @Test
    void wrongAssignedRestaurantReferenceCausesDomainRejectionWithoutChangingAuthority() {
        var repository = new InMemoryOrderRepository();
        var placed = newOrder("123e4567-e89b-12d3-a456-426614174181");
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var result = service.recordRestaurantAcceptance(
                placed.id(), new RestaurantId("unassigned-restaurant"));

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(OrderActionRejection.Code.RESTAURANT_DOES_NOT_OWN_ORDER);
        assertThat(repository.findCurrentById(placed.id())).containsSame(placed);
        // A matching ID is a local relationship check, not authenticated identity.
    }

    @Test
    void placementActorContextIsCheckedBeforeApplicationPublishesNewOrder() {
        var repository = new InMemoryOrderRepository();
        var service = new PlaceOrderService(repository, CLOCK);

        var result = service.place(new PlaceOrderCommand(
                ActorContext.customer("different-customer"),
                CUSTOMER.value(), RESTAURANT.value(),
                List.of(new PlaceOrderCommand.Line(
                        "burger-1", "Burger", 1, new BigDecimal("5.50"))
                )
        ));
        assertThat(result).isInstanceOf(PlaceOrderResult.Rejected.class);
        assertThat(repository.size()).isZero();
        // ActorContext is supplied business input, not proof of authentication.
    }

    @Test
    void placementOrchestrationPublishesOrderAndReturnsLocalPlacementFact() {
        var repository = new InMemoryOrderRepository();
        var service = new PlaceOrderService(repository, CLOCK);

        var result = service.place(new PlaceOrderCommand(
                CUSTOMER.value(), RESTAURANT.value(),
                List.of(new PlaceOrderCommand.Line(
                        "burger-1", "Burger", 1, new BigDecimal("5.50"))
                )
        ));

        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        var accepted = (PlaceOrderResult.Accepted) result;
        assertThat(accepted.fact().orderId()).isEqualTo(accepted.order().id());
        assertThat(accepted.fact().customerId()).isEqualTo(CUSTOMER);
        assertThat(accepted.fact().restaurantId()).isEqualTo(RESTAURANT);
        assertThat(repository.findCurrentById(accepted.order().id())).isPresent();
        // A local domain fact is not a published broker integration event.
    }

    @Test
    void localPaymentRecordingHasOnlyTheExistingOrderFactSemantics() {
        var repository = new InMemoryOrderRepository();
        var placed = newOrder("123e4567-e89b-12d3-a456-426614174181");
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var result = service.recordPayment(placed.id());

        assertThat(result).isInstanceOf(OrderActionResult.Accepted.class);
        var current = repository.findCurrentById(placed.id()).orElseThrow();
        assertThat(current.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(current.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
        // No provider adapter is wired into OrderWorkflowService. This test does
        // NOT demonstrate any charge, settlement, provider acknowledgement or idempotency.
    }

    @Test
    void terminalOrderCanContinueLocalRefundRequestWorkflowWithoutNewOrderIdentity() {
        var placed = newOrder("123e4567-e89b-12d3-a456-426614174181");
        var paid = placed.recordPayment(AT.plusSeconds(1));
        var cancelled = paid.recordCancellation(CUSTOMER, AT.plusSeconds(2));
        var refundRequested = cancelled.recordRefundRequest(AT.plusSeconds(3));

        assertThat(cancelled.status().isTerminal()).isTrue();
        assertThat(refundRequested.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(refundRequested.id()).isEqualTo(placed.id());
        assertThat(refundRequested.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
        assertThat(cancelled.workflowOccurrences()).hasSize(2);
        // Terminal lifecycle status != no subsequent local milestone.
        // Nothing here proves an actual provider refund.
    }
}
