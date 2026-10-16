package com.acme.foodordering.domain.inventory;

/** A local domain decision failure, not a PostgreSQL transaction outcome. */
public final class InventoryReservationPlanningException extends RuntimeException {
    public enum Code {
        WRONG_RESTAURANT,
        ORDER_NOT_PLACED,
        ITEM_NOT_IN_SNAPSHOT,
        INSUFFICIENT_OBSERVED_QUANTITY
    }
    private final Code code;
    public InventoryReservationPlanningException(Code code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code);
    }
    public Code code() { return code; }
}
