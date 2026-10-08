package com.acme.foodordering.application.port.in;

public interface ModifyOrderUseCase {
    OrderActionResult modify(ModifyOrderCommand command);
}
