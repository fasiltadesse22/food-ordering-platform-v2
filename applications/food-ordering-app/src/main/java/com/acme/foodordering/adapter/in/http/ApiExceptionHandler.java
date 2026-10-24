package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.service.OrderNotFoundException;
import com.acme.foodordering.domain.order.InvalidOrderTransitionException;
import com.acme.foodordering.domain.order.OrderTransitionContractViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Order not found");
        detail.setProperty("kind", "NOT_FOUND");
        detail.setProperty("code", "ORDER_NOT_FOUND");
        return detail;
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    ProblemDetail handleLifecycleRejection(InvalidOrderTransitionException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        detail.setTitle("Business lifecycle conflict");
        detail.setProperty("kind", "BUSINESS_REJECTION");
        detail.setProperty("code", "ILLEGAL_LIFECYCLE_TRANSITION");
        return detail;
    }

    @ExceptionHandler(InvalidWorkflowActionException.class)
    ProblemDetail handleInvalidAction(InvalidWorkflowActionException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid workflow action");
        detail.setProperty("kind", "INVALID_INPUT");
        return detail;
    }

    @ExceptionHandler(OrderTransitionContractViolationException.class)
    ProblemDetail handleContractDefect(OrderTransitionContractViolationException exception) {
        var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal request processing failed");
        detail.setTitle("Internal technical failure");
        detail.setProperty("kind", "TECHNICAL_FAILURE");
        return detail;
    }

    @ExceptionHandler(InvalidOrderRequestException.class)
    ProblemDetail handleBadRequest(InvalidOrderRequestException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid order request");
        detail.setProperty("kind", "INVALID_INPUT");
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableJson(HttpMessageNotReadableException exception) {
        var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid request body");
        detail.setTitle("Invalid request body");
        detail.setProperty("kind", "INVALID_INPUT");
        return detail;
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal request processing failed");
        detail.setTitle("Internal technical failure");
        detail.setProperty("kind", "TECHNICAL_FAILURE");
        return detail;
    }
}
