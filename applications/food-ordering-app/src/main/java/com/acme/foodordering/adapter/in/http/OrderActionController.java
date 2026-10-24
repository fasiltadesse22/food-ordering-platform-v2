package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.OrderWorkflowDecisionUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowResult;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P09 learning-only HTTP translation.
 * IMPORTANT: this is NOT an authenticated production workflow endpoint.
 * Actor attribution in earlier parts is not authentication/authorization.
 */
@RestController
@RequestMapping("/orders/{orderId}/actions")
public final class OrderActionController {
    private final OrderWorkflowDecisionUseCase useCase;

    public OrderActionController(OrderWorkflowDecisionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{action}")
    public ResponseEntity<?> execute(@PathVariable String orderId, @PathVariable String action) {
        final OrderId id;
        try {
            id = OrderId.from(orderId);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidOrderRequestException("invalid order identifier");
        }
        final OrderWorkflowAction parsed;
        try {
            parsed = OrderWorkflowAction.valueOf(action);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidWorkflowActionException(action);
        }
        return switch (useCase.execute(id, parsed)) {
            case OrderWorkflowResult.Accepted success ->
                    ResponseEntity.ok(OrderResponse.from(success.order()));
            case OrderWorkflowResult.Rejected denied ->
                    ResponseEntity.status(409).body(Map.of(
                            "kind", "BUSINESS_REJECTION",
                            "code", denied.rejection().code().name(),
                            "action", denied.rejection().action().name(),
                            "message", denied.rejection().message()
                    ));
        };
    }
}
