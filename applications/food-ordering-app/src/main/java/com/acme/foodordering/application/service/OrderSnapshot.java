package com.acme.foodordering.application.service;

import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSnapshot(
        OrderId id,
        CustomerId customerId,
        RestaurantId restaurantId,
        String status,
        Instant placedAt,
        BigDecimal total,
        List<Line> lines,
        List<WorkflowOccurrence> workflow
) {
    public OrderSnapshot {
        lines = List.copyOf(lines);
        workflow = List.copyOf(workflow);
    }

    public record Line(
            String menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record WorkflowOccurrence(
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt
    ) {
        static WorkflowOccurrence from(OrderWorkflowOccurrence occurrence) {
            return new WorkflowOccurrence(
                    occurrence.action(),
                    occurrence.participant(),
                    occurrence.occurredAt()
            );
        }
    }

    public static OrderSnapshot from(Order order) {
        return new OrderSnapshot(
                order.id(),
                order.customerId(),
                order.restaurantId(),
                order.status().name(),
                order.placedAt(),
                order.total(),
                order.lines().stream()
                        .map(line -> new Line(
                                line.menuItemId(),
                                line.name(),
                                line.quantity(),
                                line.unitPrice(),
                                line.lineTotal()
                        ))
                        .toList(),
                order.workflowOccurrences().stream()
                        .map(WorkflowOccurrence::from)
                        .toList()
        );
    }
}
