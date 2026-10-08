package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.OrderId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final OrderWorkflowUseCase workflowUseCase;

    public OrderController(
            PlaceOrderUseCase placeOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            OrderWorkflowUseCase workflowUseCase
    ) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.workflowUseCase = workflowUseCase;
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request) {
        var command = new PlaceOrderCommand(
                request.customerId(),
                request.restaurantId(),
                request.lines().stream()
                        .map(line -> new PlaceOrderCommand.Line(
                                line.menuItemId(),
                                line.name(),
                                line.quantity(),
                                line.unitPrice()
                        ))
                        .toList()
        );

        var result = placeOrderUseCase.place(command);

        return switch (result) {
            case PlaceOrderResult.Accepted accepted -> {
                var response = OrderResponse.from(accepted.order());
                yield ResponseEntity
                        .created(URI.create("/orders/" + response.id()))
                        .body(response);
            }
            case PlaceOrderResult.Rejected rejected ->
                    ResponseEntity.unprocessableEntity()
                            .body(PlaceOrderRejectedResponse.from(rejected.rejection()));
        };
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable String orderId) {
        return OrderResponse.from(getOrderUseCase.get(OrderId.from(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancel(
            @PathVariable String orderId,
            @RequestBody CancelOrderRequest request
    ) {
        var result = workflowUseCase.recordCancellation(
                OrderId.from(orderId),
                new CustomerId(request.customerId())
        );
        return toHttpResponse(result);
    }

    private static ResponseEntity<?> toHttpResponse(OrderActionResult result) {
        return switch (result) {
            case OrderActionResult.Accepted accepted ->
                    ResponseEntity.ok(OrderResponse.from(accepted.order()));
            case OrderActionResult.Rejected rejected -> {
                var status = statusFor(rejected.rejection().code());
                yield ResponseEntity
                        .status(status)
                        .body(OrderActionRejectedResponse.from(rejected.rejection()));
            }
        };
    }

    private static HttpStatus statusFor(OrderActionRejection.Code code) {
        return switch (code) {
            case ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CUSTOMER_DOES_NOT_OWN_ORDER,
                 RESTAURANT_DOES_NOT_OWN_ORDER -> HttpStatus.FORBIDDEN;
            case ILLEGAL_TRANSITION,
                 REFUND_REQUIRES_RECORDED_PAYMENT,
                 REFUND_REQUIRES_REJECTED_OR_CANCELLED_ORDER,
                 REFUND_ALREADY_REQUESTED,
                 MODIFICATION_REQUIRES_PLACED_ORDER -> HttpStatus.CONFLICT;
        };
    }
}
