package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;

public interface GetOrderUseCase {
    OrderSnapshot get(String orderId);
}
