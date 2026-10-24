package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import java.util.Objects;

/** P09: routine business decisions are values, never synthetic infrastructure errors. */
public sealed interface OrderWorkflowResult
        permits OrderWorkflowResult.Accepted, OrderWorkflowResult.Rejected {
    record Accepted(OrderSnapshot order) implements OrderWorkflowResult {
        public Accepted { Objects.requireNonNull(order, "order"); }
    }
    record Rejected(BusinessRejection rejection) implements OrderWorkflowResult {
        public Rejected { Objects.requireNonNull(rejection, "rejection"); }
    }
    record BusinessRejection(
            Code code, OrderWorkflowAction action, String message
    ) {
        public enum Code { ILLEGAL_LIFECYCLE_TRANSITION }
        public BusinessRejection {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(action, "action");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("rejection message must not be blank");
            }
        }
    }
}
