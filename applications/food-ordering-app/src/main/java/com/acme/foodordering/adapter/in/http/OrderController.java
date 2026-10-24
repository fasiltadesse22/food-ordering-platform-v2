package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.domain.order.OrderId;
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

    public OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request) {
        if (request == null || request.customerId() == null || request.customerId().isBlank()
                || request.restaurantId() == null || request.restaurantId().isBlank()
                || request.lines() == null || request.lines().isEmpty()) {
            throw new InvalidOrderRequestException("order requires customer, restaurant and lines");
        }
        for (var line : request.lines()) {
            if (line == null || line.menuItemId() == null || line.menuItemId().isBlank()
                    || line.name() == null || line.name().isBlank()
                    || line.quantity() <= 0 || line.unitPrice() == null
                    || line.unitPrice().signum() < 0) {
                throw new InvalidOrderRequestException("invalid order line");
            }
        }
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
        try {
            return OrderResponse.from(getOrderUseCase.get(OrderId.from(orderId)));
        } catch (IllegalArgumentException invalid) {
            throw new InvalidOrderRequestException("invalid order identifier");
        }
    }
}
