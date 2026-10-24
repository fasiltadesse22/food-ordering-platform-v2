package com.acme.foodordering.adapter.in.http;

/** Invalid action name at the transport boundary, not a business transition denial. */
public final class InvalidWorkflowActionException extends RuntimeException {
    public InvalidWorkflowActionException(String action) {
        super("unsupported workflow action: " + action);
    }
}
