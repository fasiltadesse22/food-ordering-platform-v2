package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.ActorType;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.RestaurantId;

import java.time.Clock;
import java.util.Objects;

public final class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository repository;
    private final Clock clock;

    public PlaceOrderService(OrderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OrderSnapshot place(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        requireCustomerActorForRequestedCustomer(command);

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

        repository.save(order);
        return OrderSnapshot.from(order);
    }

    private static void requireCustomerActorForRequestedCustomer(PlaceOrderCommand command) {
        if (command.actor().actorType() != ActorType.CUSTOMER) {
            throw new ActorNotAllowedException(
                    "actor type " + command.actor().actorType() + " cannot place a customer order"
            );
        }

        if (!command.actor().actorId().equals(command.customerId())) {
            throw new ActorNotAllowedException(
                    "customer actor must place an order for the same customer id"
            );
        }
    }
}
