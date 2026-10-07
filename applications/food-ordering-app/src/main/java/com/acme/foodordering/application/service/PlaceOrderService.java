package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.ActorType;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderRejection;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.fact.OrderPlaced;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository repository;
    private final Clock clock;

    public PlaceOrderService(OrderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public PlaceOrderResult place(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        var rejection = decideActorEligibility(command);
        if (rejection.isPresent()) {
            return new PlaceOrderResult.Rejected(rejection.get());
        }

        var order = Order.place(
                OrderId.random(),
                new CustomerId(command.customerId()),
                new RestaurantId(command.restaurantId()),
                command.lines().stream()
                        .map(line -> new OrderLine(
                                line.menuItemId(),
                                line.name(),
                                line.quantity(),
                                line.unitPrice()
                        ))
                        .toList(),
                clock.instant()
        );

        repository.saveCurrent(order);

        var fact = OrderPlaced.from(order);
        return new PlaceOrderResult.Accepted(OrderSnapshot.from(order), fact);
    }

    private static Optional<PlaceOrderRejection> decideActorEligibility(PlaceOrderCommand command) {
        if (command.actor().actorType() != ActorType.CUSTOMER) {
            return Optional.of(new PlaceOrderRejection(
                    PlaceOrderRejection.Code.ACTOR_TYPE_NOT_ALLOWED,
                    "actor type " + command.actor().actorType() + " cannot place a customer order"
            ));
        }

        if (!command.actor().actorId().equals(command.customerId())) {
            return Optional.of(new PlaceOrderRejection(
                    PlaceOrderRejection.Code.ACTOR_CUSTOMER_MISMATCH,
                    "customer actor must place an order for the same customer id"
            ));
        }

        return Optional.empty();
    }
}
