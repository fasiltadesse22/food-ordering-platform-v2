package com.acme.foodordering.domain.inventory;

import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain service involving independently meaningful Order and Inventory
 * availability concepts. It is PURE and computes only a PROPOSAL.
 *
 * It cannot reserve stock: snapshots can be stale immediately. A real
 * authoritative inventory writer must atomically admit each quantity,
 * correlate the operation, and define the Order coordination boundary.
 */
public final class OrderInventoryReservationPolicy {
    public InventoryReservationPlan propose(
            Order order,
            InventoryAvailabilitySnapshot snapshot
    ) {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!order.restaurantId().equals(snapshot.restaurantId()))
            throw failure(InventoryReservationPlanningException.Code.WRONG_RESTAURANT,
                    "inventory snapshot belongs to another restaurant");
        if (order.status() != OrderStatus.PLACED)
            throw failure(InventoryReservationPlanningException.Code.ORDER_NOT_PLACED,
                    "only a PLACED Order can be proposed for inventory allocation");

        // One Order can contain repeated menu item identifiers. Aggregate
        // required quantities FIRST; otherwise each line may be individually
        // below available stock while their combined demand exceeds it.
        Map<String,Integer> requested = new LinkedHashMap<>();
        order.lines().forEach(line ->
                requested.merge(line.menuItemId(),line.quantity(),Math::addExact));

        requested.forEach((item,quantity) -> {
            Integer available = snapshot.availableByMenuItem().get(item);
            if (available == null)
                throw failure(InventoryReservationPlanningException.Code.ITEM_NOT_IN_SNAPSHOT,
                        "observed stock has no item " + item);
            if (quantity > available)
                throw failure(InventoryReservationPlanningException.Code.INSUFFICIENT_OBSERVED_QUANTITY,
                        "observed stock insufficient for item " + item);
        });

        return new InventoryReservationPlan(
                order.id(),order.restaurantId(),snapshot.observedVersion(),requested);
    }

    private static InventoryReservationPlanningException failure(
            InventoryReservationPlanningException.Code code, String message) {
        return new InventoryReservationPlanningException(code,message);
    }
}
