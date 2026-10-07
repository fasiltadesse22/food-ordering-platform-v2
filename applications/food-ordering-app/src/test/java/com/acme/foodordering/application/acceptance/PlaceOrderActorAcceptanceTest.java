package com.acme.foodordering.application.acceptance;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.ActorContext;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.service.ActorNotAllowedException;
import com.acme.foodordering.application.service.PlaceOrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceOrderActorAcceptanceTest {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private final PlaceOrderService service = new PlaceOrderService(repository, clock);

    @Test
    void customerCanPlaceAnOrderForThemself() {
        var result = service.place(command(
                ActorContext.customer("customer-1"),
                "customer-1"
        ));

        assertThat(result.status()).isEqualTo("PLACED");
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void restaurantOperatorCannotExecuteTheCustomerPlaceOrderUseCase() {
        assertThatThrownBy(() -> service.place(command(
                ActorContext.restaurantOperator("restaurant-operator-1"),
                "customer-1"
        )))
                .isInstanceOf(ActorNotAllowedException.class)
                .hasMessageContaining("RESTAURANT_OPERATOR");

        assertThat(repository.size()).isZero();
    }

    @Test
    void customerCannotPlaceAnOrderForADifferentCustomerIdentity() {
        assertThatThrownBy(() -> service.place(command(
                ActorContext.customer("customer-2"),
                "customer-1"
        )))
                .isInstanceOf(ActorNotAllowedException.class)
                .hasMessage("customer actor must place an order for the same customer id");

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
