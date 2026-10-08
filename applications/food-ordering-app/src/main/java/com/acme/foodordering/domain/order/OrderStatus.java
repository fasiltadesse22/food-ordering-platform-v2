package com.acme.foodordering.domain.order;

public enum OrderStatus {
    PLACED(false),
    ACCEPTED(false),
    REJECTED(true),
    CANCELLED(true),
    PREPARING(false),
    COMPLETED(true);

    private final boolean terminal;

    OrderStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * Terminal for the Order lifecycle means no further OrderStatus transition
     * is currently modeled from this state.
     *
     * It does not mean no later workflow/compensating action may occur.
     */
    public boolean isTerminal() {
        return terminal;
    }
}
