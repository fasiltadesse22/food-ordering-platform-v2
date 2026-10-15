package com.acme.foodordering.application.port.out;
public class OrderPersistenceException extends RuntimeException {
    public OrderPersistenceException(String message, Throwable cause) {super(message,cause);}
}
