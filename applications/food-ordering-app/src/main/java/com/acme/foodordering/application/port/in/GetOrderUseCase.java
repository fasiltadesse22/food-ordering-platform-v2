package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.OrderId;

public interface GetOrderUseCase {
    OrderSnapshot get(OrderId orderId);
}
