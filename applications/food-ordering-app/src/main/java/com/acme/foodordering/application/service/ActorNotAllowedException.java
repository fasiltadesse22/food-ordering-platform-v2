package com.acme.foodordering.application.service;

public final class ActorNotAllowedException extends RuntimeException {

    public ActorNotAllowedException(String message) {
        super(message);
    }
}
