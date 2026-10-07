package com.acme.foodordering.application.port.in;

import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.domain.order.fact.OrderPlaced;

import java.util.Objects;

public sealed interface PlaceOrderResult
        permits PlaceOrderResult.Accepted, PlaceOrderResult.Rejected {

    record Accepted(
            OrderSnapshot order,
            OrderPlaced fact
    ) implements PlaceOrderResult {
        public Accepted {
            Objects.requireNonNull(order, "order must not be null");
            Objects.requireNonNull(fact, "fact must not be null");
        }
    }

    record Rejected(
            PlaceOrderRejection rejection
    ) implements PlaceOrderResult {
        public Rejected {
            Objects.requireNonNull(rejection, "rejection must not be null");
        }
    }
}
