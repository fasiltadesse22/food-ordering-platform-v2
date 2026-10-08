package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.ModifyOrderCommand;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.ModifyOrderService;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModifyOrderUseCaseTest {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-08T10:15:00Z"),
            ZoneOffset.UTC
    );
    private final PlaceOrderService placeOrder = new PlaceOrderService(repository, clock);
    private final ModifyOrderService modifyOrder = new ModifyOrderService(repository, clock);

    @Test
    void modifyingPlacedOrderReturnsAcceptedAndUpdatesCurrentRepresentation() {
        var orderId = place();

        var result = modifyOrder.modify(new ModifyOrderCommand(
                orderId,
                new CustomerId("customer-1"),
                List.of(new ModifyOrderCommand.Line(
                        "pizza-1",
                        "Margherita",
                        2,
                        new BigDecimal("7.00")
                ))
        ));

        assertThat(result).isInstanceOf(OrderActionResult.Accepted.class);
        var modified = ((OrderActionResult.Accepted) result).order();

        assertThat(modified.status()).isEqualTo("PLACED");
        assertThat(modified.total()).isEqualByComparingTo("14.00");
        assertThat(modified.workflow())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED);

        var authoritative = repository.findCurrentById(orderId).orElseThrow();
        assertThat(authoritative.total()).isEqualByComparingTo("14.00");
    }

    @Test
    void terminalModificationReturnsBusinessRejectionAndPreservesAuthority() {
        var orderId = place();
        var current = repository.findCurrentById(orderId).orElseThrow();
        repository.saveCurrent(current.recordCancellation(
                new CustomerId("customer-1"),
                clock.instant()
        ));
        var before = repository.findCurrentById(orderId).orElseThrow();

        var result = modifyOrder.modify(new ModifyOrderCommand(
                orderId,
                new CustomerId("customer-1"),
                List.of(new ModifyOrderCommand.Line(
                        "pizza-1",
                        "Margherita",
                        1,
                        new BigDecimal("7.00")
                ))
        ));

        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(((OrderActionResult.Rejected) result).rejection().code())
                .isEqualTo(
                        OrderActionRejection.Code.MODIFICATION_REQUIRES_PLACED_ORDER
                );

        var after = repository.findCurrentById(orderId).orElseThrow();
        assertThat(after).isSameAs(before);
        assertThat(after.status().name()).isEqualTo("CANCELLED");
    }

    private com.acme.foodordering.domain.order.OrderId place() {
        var result = placeOrder.place(new PlaceOrderCommand(
                "customer-1",
                "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                ))
        ));
        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        return ((PlaceOrderResult.Accepted) result).order().id();
    }
}
