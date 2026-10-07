package com.acme.foodordering.application.port.out;

import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;

import java.util.Optional;

/**
 * Current-state authority for Order within the application boundary.
 *
 * The repository is authoritative for the application's current in-process Order
 * representation. It is not a durable database guarantee and it is not authority
 * for restaurant, payment, customer-account, or other external truths.
 */
public interface OrderRepository {

    void saveCurrent(Order order);

    Optional<Order> findCurrentById(OrderId orderId);
}
