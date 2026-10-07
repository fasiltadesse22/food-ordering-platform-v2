package com.acme.foodordering.application.port.in;

public interface PlaceOrderUseCase {
    PlaceOrderResult place(PlaceOrderCommand command);
}
