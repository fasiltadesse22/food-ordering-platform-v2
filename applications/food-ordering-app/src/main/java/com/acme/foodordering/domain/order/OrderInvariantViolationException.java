package com.acme.foodordering.domain.order;

/**
 * Signals an internally inconsistent Order state/history combination.
 *
 * This is not a normal customer/business rejection. Public domain operations
 * should reject invalid requests before constructing an invariant-violating
 * Order. Reaching this exception indicates a programming/data-integrity defect.
 */
public final class OrderInvariantViolationException extends IllegalStateException {

    public enum Code {
        CANCELLED_FACT_REQUIRES_CANCELLED_STATE,
        REJECTED_FACT_REQUIRES_REJECTED_STATE,
        COMPLETION_FACT_REQUIRES_COMPLETED_STATE,
        PREPARATION_FACT_REQUIRES_PREPARING_OR_COMPLETED_STATE,
        ACCEPTED_STATE_REQUIRES_ACCEPTANCE_FACT,
        PREPARING_STATE_REQUIRES_ACCEPTANCE_AND_PREPARATION_FACTS,
        COMPLETED_STATE_REQUIRES_ACCEPTANCE_PREPARATION_AND_COMPLETION_FACTS
    }

    private final Code code;

    OrderInvariantViolationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
