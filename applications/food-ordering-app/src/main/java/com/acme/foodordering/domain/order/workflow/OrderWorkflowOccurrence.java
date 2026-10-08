package com.acme.foodordering.domain.order.workflow;

import java.time.Instant;
import java.util.Objects;

/**
 * One recorded occurrence in the P05 workflow trace.
 *
 * It is not a durable event-store record or integration message.
 */
public record OrderWorkflowOccurrence(
        OrderWorkflowAction action,
        WorkflowParticipant participant,
        Instant occurredAt
) {
    public OrderWorkflowOccurrence {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(participant, "participant must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
