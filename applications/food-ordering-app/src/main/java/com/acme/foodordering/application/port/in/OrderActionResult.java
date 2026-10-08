package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;

import java.util.Objects;

public sealed interface OrderActionResult
        permits OrderActionResult.Accepted, OrderActionResult.Rejected {

    record Accepted(OrderSnapshot order) implements OrderActionResult {
        public Accepted {
            Objects.requireNonNull(order, "order must not be null");
        }
    }

    record Rejected(OrderActionRejection rejection) implements OrderActionResult {
        public Rejected {
            Objects.requireNonNull(rejection, "rejection must not be null");
        }
    }
}
