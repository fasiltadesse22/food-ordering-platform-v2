package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.ActorContext;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderRejection;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.PlaceOrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderSemanticFlowTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptedCommandProducesARecordedOrderAndAnOrderPlacedFact() {
        var repository = new InMemoryOrderRepository();
        var service = new PlaceOrderService(repository, clock);

        var command = command(
                ActorContext.customer("customer-1"),
                "customer-1"
        );

        var result = service.place(command);

        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        var accepted = (PlaceOrderResult.Accepted) result;

        assertThat(repository.size()).isEqualTo(1);
        assertThat(accepted.fact().orderId())
                .isEqualTo(accepted.order().id());
        assertThat(accepted.fact().occurredAt())
                .isEqualTo(Instant.parse("2026-10-07T10:00:00Z"));
    }

    @Test
    void rejectedCommandProducesARejectionOutcomeWithoutOrderStateOrOrderPlacedFact() {
        var repository = new InMemoryOrderRepository();
        var service = new PlaceOrderService(repository, clock);

        var command = command(
                ActorContext.restaurantOperator("restaurant-operator-1"),
                "customer-1"
        );

        var result = service.place(command);

        assertThat(result).isInstanceOf(PlaceOrderResult.Rejected.class);
        var rejected = (PlaceOrderResult.Rejected) result;

        assertThat(rejected.rejection().code())
                .isEqualTo(PlaceOrderRejection.Code.ACTOR_TYPE_NOT_ALLOWED);
        assertThat(repository.size()).isZero();
    }

    private static PlaceOrderCommand command(ActorContext actor, String customerId) {
        return new PlaceOrderCommand(
                actor,
                customerId,
                "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "burger-1",
                        "Classic Burger",
                        2,
                        new BigDecimal("5.50")
                ))
        );
    }
}
