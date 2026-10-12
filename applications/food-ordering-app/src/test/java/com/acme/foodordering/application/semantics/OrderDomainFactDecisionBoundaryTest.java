package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.ActorContext;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.fact.OrderPlaced;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class OrderDomainFactDecisionBoundaryTest {
    private static final Instant AT=Instant.parse("2026-10-12T12:45:00Z");
    private static final Clock CLOCK=Clock.fixed(AT,ZoneOffset.UTC);
    private static final CustomerId CUSTOMER=new CustomerId("customer-facts");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-facts");
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174199");

    private static OrderLine line(String price) {
        return new OrderLine("burger-1","Burger",2,new BigDecimal(price));
    }
    private static Order initial() {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(line("5.50")),AT);
    }
    private static PlaceOrderCommand command() {
        return new PlaceOrderCommand(CUSTOMER.value(),RESTAURANT.value(),
                List.of(new PlaceOrderCommand.Line("burger-1","Burger",2,new BigDecimal("5.50"))));
    }

    @Test void acceptedPlacementReturnsFactMatchingPublishedInitialRepresentation() {
        var repository=new InMemoryOrderRepository();
        var result=new PlaceOrderService(repository,CLOCK).place(command());
        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        var accepted=(PlaceOrderResult.Accepted)result;
        var current=repository.findCurrentById(accepted.order().id()).orElseThrow();
        assertThat(current.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(current.workflowOccurrences()).isEmpty();
        assertThat(accepted.fact().orderId()).isEqualTo(current.id());
        assertThat(accepted.fact().customerId()).isEqualTo(current.customerId());
        assertThat(accepted.fact().restaurantId()).isEqualTo(current.restaurantId());
        assertThat(accepted.fact().occurredAt()).isEqualTo(current.placedAt());
        assertThat(accepted.fact().total()).isEqualByComparingTo(current.total());
        // No publisher, broker or event store participates in this use case.
    }

    @Test void rejectedPlacementProducesNoAcceptedFactOrNewCurrentOrder() {
        var repository=new InMemoryOrderRepository();
        var rejectedCommand=new PlaceOrderCommand(
                ActorContext.customer("another-customer"),CUSTOMER.value(),
                RESTAURANT.value(),command().lines());
        var result=new PlaceOrderService(repository,CLOCK).place(rejectedCommand);
        assertThat(result).isInstanceOf(PlaceOrderResult.Rejected.class);
        assertThat(repository.size()).isZero();
    }

    @Test void placementFactFactoryRejectsLaterAcceptedState() {
        var accepted=initial().recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        assertThatThrownBy(()->OrderPlaced.from(accepted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initial PLACED representation");
    }

    @Test void placementFactFactoryRejectsModifiedButStillPlacedState() {
        var modified=initial().modifyLines(CUSTOMER,
                List.of(line("7.00")),AT.plusSeconds(2));
        assertThat(modified.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(modified.total()).isEqualByComparingTo("14.00");
        assertThat(modified.workflowOccurrences())
                .extracting(o->o.action()).containsExactly(OrderWorkflowAction.ORDER_MODIFIED);
        assertThatThrownBy(()->OrderPlaced.from(modified))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void placementFactFactoryRejectsPaidMilestoneAtStillPlacedState() {
        var recorded=initial().recordPayment(AT.plusSeconds(1));
        assertThat(recorded.status()).isEqualTo(OrderStatus.PLACED);
        assertThatThrownBy(()->OrderPlaced.from(recorded))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void historicalPlacementFactRemainsUnchangedAfterOrderModification() {
        var first=initial();
        var placementFact=OrderPlaced.from(first);
        var modified=first.modifyLines(CUSTOMER,
                List.of(line("7.00")),AT.plusSeconds(2));
        assertThat(placementFact.total()).isEqualByComparingTo("11.00");
        assertThat(placementFact.occurredAt()).isEqualTo(AT);
        assertThat(modified.total()).isEqualByComparingTo("14.00");
        assertThat(modified.id()).isEqualTo(placementFact.orderId());
    }

    @Test void failureBeforeRepositoryPublicationDoesNotReturnAcceptedFact() {
        var repository=new FaultInjectingRepository(false);
        assertThatThrownBy(()->new PlaceOrderService(repository,CLOCK).place(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected before save");
        assertThat(repository.published).isNull();
    }

    @Test void failureAfterInMemoryPublicationLeavesUnknownCallerOutcome() {
        var repository=new FaultInjectingRepository(true);
        assertThatThrownBy(()->new PlaceOrderService(repository,CLOCK).place(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected after save");
        assertThat(repository.published).isNotNull();
        assertThat(repository.published.status()).isEqualTo(OrderStatus.PLACED);
        // The caller sees an exception, yet the repository has published state.
        // This does NOT prove a real database commit or a lost HTTP response.
    }

    @Test void workflowOccurrenceExplainsTransitionButIsNotIndependentFactPublication() {
        var first=initial();
        var accepted=first.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(3));
        assertThat(first.workflowOccurrences()).isEmpty();
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.workflowOccurrences())
                .extracting(o->o.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        // Immutable successor carries the occurrence; no broker event is emitted.
    }

    private static final class FaultInjectingRepository implements OrderRepository {
        private final boolean publishThenFail;
        private Order published;
        private FaultInjectingRepository(boolean publishThenFail) {
            this.publishThenFail=publishThenFail;
        }
        @Override public void saveCurrent(Order order) {
            if(!publishThenFail)throw new IllegalStateException("injected before save");
            published=order;
            throw new IllegalStateException("injected after save");
        }
        @Override public Optional<Order> findCurrentById(OrderId id) {
            return published!=null && published.id().equals(id)
                    ?Optional.of(published):Optional.empty();
        }
    }
}
