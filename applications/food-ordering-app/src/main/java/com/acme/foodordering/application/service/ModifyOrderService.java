package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.ModifyOrderCommand;
import com.acme.foodordering.application.port.in.ModifyOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.OrderLine;

import java.time.Clock;
import java.util.Objects;

public final class ModifyOrderService implements ModifyOrderUseCase {

    private final OrderRepository repository;
    private final Clock clock;

    public ModifyOrderService(OrderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OrderSnapshot modify(ModifyOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        var current = repository.findCurrentById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId().toString()));

        var updated = current.modifyLines(
                command.actingCustomerId(),
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

        repository.saveCurrent(updated);
        return OrderSnapshot.from(updated);
    }
}
