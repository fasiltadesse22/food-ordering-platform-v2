package com.acme.foodordering.domain.order;

import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;

/**
 * P08 read-only semantics of fulfillment closure and *local* financial
 * follow-up; deliberately no financial effect or Saga coordinator.
 *
 * The absence of PAYMENT_RECORDED is not evidence that a provider never
 * captured funds. Refund eligibility here means "may record one local
 * refund-request milestone under the provisional P06/P07 policy".
 */
public record OrderClosure(
        FulfillmentClosure fulfillment,
        RefundRequestReadiness refundRequest,
        boolean paymentRecorded,
        boolean refundRequestRecorded
) {
    public enum FulfillmentClosure {
        ACTIVE,
        RESTAURANT_REJECTED,
        CUSTOMER_CANCELLED,
        FULFILLMENT_COMPLETED
    }

    public enum RefundRequestReadiness {
        NOT_A_CLOSED_REFUND_PATH,
        NO_LOCAL_PAYMENT_EVIDENCE,
        MAY_RECORD_REQUEST,
        REQUEST_ALREADY_RECORDED
    }

    public boolean fulfillmentTerminal() {
        return fulfillment != FulfillmentClosure.ACTIVE;
    }

    /**
     * No implicit "undo" API: closure and historical milestones cannot be
     * deleted by a regular Order transition.
     */
    public boolean permitsAutomaticFulfillmentReopen() {
        return false;
    }

    public boolean mayRecordRefundRequest() {
        return refundRequest == RefundRequestReadiness.MAY_RECORD_REQUEST;
    }

    public static OrderClosure from(Order order) {
        var history = order.workflowOccurrences();
        boolean paid = history.stream().anyMatch(
                occurrence -> occurrence.action() == OrderWorkflowAction.PAYMENT_RECORDED);
        boolean refund = history.stream().anyMatch(
                occurrence -> occurrence.action() == OrderWorkflowAction.REFUND_REQUESTED);

        FulfillmentClosure closure = switch (order.status()) {
            case PLACED, ACCEPTED, PREPARING -> FulfillmentClosure.ACTIVE;
            case REJECTED -> FulfillmentClosure.RESTAURANT_REJECTED;
            case CANCELLED -> FulfillmentClosure.CUSTOMER_CANCELLED;
            case COMPLETED -> FulfillmentClosure.FULFILLMENT_COMPLETED;
        };

        RefundRequestReadiness readiness;
        if (closure != FulfillmentClosure.RESTAURANT_REJECTED
                && closure != FulfillmentClosure.CUSTOMER_CANCELLED) {
            readiness = RefundRequestReadiness.NOT_A_CLOSED_REFUND_PATH;
        } else if (!paid) {
            readiness = RefundRequestReadiness.NO_LOCAL_PAYMENT_EVIDENCE;
        } else if (refund) {
            readiness = RefundRequestReadiness.REQUEST_ALREADY_RECORDED;
        } else {
            readiness = RefundRequestReadiness.MAY_RECORD_REQUEST;
        }
        return new OrderClosure(closure, readiness, paid, refund);
    }
}
