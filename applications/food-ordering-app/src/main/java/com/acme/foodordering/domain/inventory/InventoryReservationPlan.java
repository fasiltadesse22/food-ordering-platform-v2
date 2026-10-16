package com.acme.foodordering.domain.inventory;

import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;
import java.util.Map;
import java.util.Objects;

/**
 * Proposed allocation derived from an observed stock snapshot.
 * This is NOT a confirmed reservation or authoritative stock decrement.
 */
public record InventoryReservationPlan(
        OrderId orderId,
        RestaurantId restaurantId,
        long observedInventoryVersion,
        Map<String, Integer> requestedByMenuItem
) {
    public InventoryReservationPlan {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(restaurantId, "restaurantId");
        if (observedInventoryVersion < 0) throw new IllegalArgumentException("observedInventoryVersion");
        Objects.requireNonNull(requestedByMenuItem, "requestedByMenuItem");
        requestedByMenuItem = Map.copyOf(requestedByMenuItem);
        if (requestedByMenuItem.isEmpty()) throw new IllegalArgumentException("empty allocation proposal");
        requestedByMenuItem.forEach((item, quantity) -> {
            if (item == null || item.isBlank() || quantity == null || quantity <= 0)
                throw new IllegalArgumentException("invalid requested item/quantity");
        });
    }
}
