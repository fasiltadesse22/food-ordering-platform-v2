package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Objects;

public final class GetOrderService implements GetOrderUseCase {

    private final OrderRepository repository;

    public GetOrderService(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public OrderSnapshot get(OrderId orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        return repository.findCurrentById(orderId)
                .map(OrderSnapshot::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
    }
}
