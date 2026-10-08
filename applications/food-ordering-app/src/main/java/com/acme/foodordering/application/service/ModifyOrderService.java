package com.acme.foodordering.application.service;

import com.acme.foodordering.application.port.in.ModifyOrderCommand;
import com.acme.foodordering.application.port.in.ModifyOrderUseCase;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.domain.order.OrderGuardViolationException;
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
    public OrderActionResult modify(ModifyOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        var current = repository.findCurrentById(command.orderId());
        if (current.isEmpty()) {
            return new OrderActionResult.Rejected(new OrderActionRejection(
                    OrderActionRejection.Code.ORDER_NOT_FOUND,
                    "order " + command.orderId() + " was not found"
            ));
        }

        try {
            var updated = current.get().modifyLines(
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
            return new OrderActionResult.Accepted(OrderSnapshot.from(updated));
        } catch (OrderGuardViolationException exception) {
            var code = switch (exception.code()) {
                case CUSTOMER_DOES_NOT_OWN_ORDER ->
                        OrderActionRejection.Code.CUSTOMER_DOES_NOT_OWN_ORDER;
                case MODIFICATION_REQUIRES_PLACED_ORDER ->
                        OrderActionRejection.Code.MODIFICATION_REQUIRES_PLACED_ORDER;
                default -> throw exception;
            };

            return new OrderActionResult.Rejected(new OrderActionRejection(
                    code,
                    exception.getMessage()
            ));
        }
    }
}
