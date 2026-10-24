package com.acme.foodordering.adapter.in.http;

/** Request syntax/structure failure, not an Order lifecycle business denial. */
public final class InvalidOrderRequestException extends RuntimeException {
    public InvalidOrderRequestException(String message) { super(message); }
}
