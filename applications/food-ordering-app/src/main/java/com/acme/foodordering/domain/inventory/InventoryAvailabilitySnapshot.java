package com.acme.foodordering.domain.inventory;

import com.acme.foodordering.domain.order.RestaurantId;
import java.util.Map;
import java.util.Objects;

/** Immutable observed availability only: NOT an allocation or a reservation. */
public record InventoryAvailabilitySnapshot(
        RestaurantId restaurantId,
        long observedVersion,
        Map<String, Integer> availableByMenuItem
) {
    public InventoryAvailabilitySnapshot {
        Objects.requireNonNull(restaurantId, "restaurantId");
        if (observedVersion < 0) throw new IllegalArgumentException("observedVersion must be nonnegative");
        Objects.requireNonNull(availableByMenuItem, "availableByMenuItem");
        availableByMenuItem = Map.copyOf(availableByMenuItem);
        availableByMenuItem.forEach((item, available) -> {
            if (item == null || item.isBlank())
                throw new IllegalArgumentException("menuItemId must not be blank");
            if (available == null || available < 0)
                throw new IllegalArgumentException("availability must be nonnegative");
        });
    }
}
