package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.WorkflowParticipant;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * P07: one independently testable specification of business preconditions and
 * observable successor postconditions. Not an optimistic lock or database transaction.
 */
final class OrderTransitionContract {
    private OrderTransitionContract() {}

    /** Only a current domain Order representation may authorize these local operations. */
    static OrderStatus requirePreconditions(Order before, OrderWorkflowAction action) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(action, "action");
        OrderStatus status = before.status();
        return switch (action) {
            case PAYMENT_RECORDED -> {
                if ((status != OrderStatus.PLACED && status != OrderStatus.ACCEPTED)
                        || has(before, OrderWorkflowAction.PAYMENT_RECORDED)) {
                    throw illegal(status, action, "payment is recordable once before preparation");
                }
                yield status;
            }
            case RESTAURANT_ACCEPTED -> {
                require(status, OrderStatus.PLACED, action);
                yield OrderStatus.ACCEPTED;
            }
            case RESTAURANT_REJECTED -> {
                require(status, OrderStatus.PLACED, action);
                yield OrderStatus.REJECTED;
            }
            case ORDER_CANCELLED -> {
                if (status != OrderStatus.PLACED && status != OrderStatus.ACCEPTED) {
                    throw illegal(status, action, "cancellation only while placed or accepted");
                }
                yield OrderStatus.CANCELLED;
            }
            case PREPARATION_STARTED -> {
                require(status, OrderStatus.ACCEPTED, action);
                if (!has(before, OrderWorkflowAction.PAYMENT_RECORDED)) {
                    throw illegal(status, action, "this provisional P07 policy requires recorded payment");
                }
                yield OrderStatus.PREPARING;
            }
            case ORDER_COMPLETED -> {
                require(status, OrderStatus.PREPARING, action);
                yield OrderStatus.COMPLETED;
            }
            case REFUND_REQUESTED -> {
                if (status != OrderStatus.CANCELLED && status != OrderStatus.REJECTED) {
                    throw illegal(status, action, "refund request follows a cancelled or rejected order");
                }
                if (!has(before, OrderWorkflowAction.PAYMENT_RECORDED)
                        || has(before, OrderWorkflowAction.REFUND_REQUESTED)) {
                    throw illegal(status, action, "requires prior recorded payment and no earlier request");
                }
                yield status;
            }
        };
    }

    /**
     * A *developer contract failure* rather than expected business rejection.
     * After a legal decision, ensure successor state and accepted history agree.
     */
    static void requirePostconditions(
            Order before,
            Order after,
            OrderWorkflowAction action,
            WorkflowParticipant participant,
            Instant occurredAt,
            OrderStatus expected
    ) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(participant, "participant");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(expected, "expected");

        // Keep business identity, owner references and accepted Order contents stable.
        check(before.id().equals(after.id()), "OrderId changed");
        check(before.customerId().equals(after.customerId()), "customer changed");
        check(before.restaurantId().equals(after.restaurantId()), "restaurant changed");
        check(before.placedAt().equals(after.placedAt()), "placedAt changed");
        check(before.lines().equals(after.lines()), "order lines changed");
        check(before.total().compareTo(after.total()) == 0, "total changed");
        check(after.status() == expected, "target state differs from accepted decision");

        List<OrderWorkflowOccurrence> prior = before.workflowOccurrences();
        List<OrderWorkflowOccurrence> later = after.workflowOccurrences();
        check(later.size() == prior.size() + 1, "not exactly one accepted occurrence");
        check(later.subList(0, prior.size()).equals(prior), "prior accepted history changed");
        OrderWorkflowOccurrence appended = later.get(later.size() - 1);
        check(appended.action() == action, "recorded action differs from attempted action");
        check(appended.participant() == participant, "recorded participant differs");
        check(appended.occurredAt().equals(occurredAt), "recorded timestamp differs");
    }

    private static void check(boolean ok, String reason) {
        if (!ok) {
            throw new OrderTransitionContractViolationException(reason);
        }
    }

    private static boolean has(Order o, OrderWorkflowAction action) {
        return o.workflowOccurrences().stream().anyMatch(x -> x.action() == action);
    }

    private static void require(OrderStatus actual, OrderStatus needed, OrderWorkflowAction action) {
        if (actual != needed) {
            throw illegal(actual, action, "requires source state " + needed);
        }
    }

    private static InvalidOrderTransitionException illegal(
            OrderStatus source, OrderWorkflowAction action, String reason
    ) {
        return new InvalidOrderTransitionException(source, action, reason);
    }
}
