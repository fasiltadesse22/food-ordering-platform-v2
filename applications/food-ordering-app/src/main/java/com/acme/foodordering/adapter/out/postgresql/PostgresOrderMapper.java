package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.InvalidPersistedOrderException;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts relational rows to the existing immutable Order aggregate.
 *
 * This is persistence mapping, not a business operation or an independent
 * domain identity model. Positions are persisted Java list indexes: a gap
 * cannot be silently compressed when reconstructing the authoritative list.
 *
 * Transaction-level read snapshot selection is intentionally NOT altered
 * here; the actual READ COMMITTED cross-query failure belongs to P16.
 */
final class PostgresOrderMapper {
    private PostgresOrderMapper() {}

    static Order reconstitute(
            OrderId id, String customer, String restaurant, String status,
            Instant placedAt, List<OrderLine> lines,
            List<OrderWorkflowOccurrence> history) {
        try {
            return Order.reconstitute(
                    id, new CustomerId(customer), new RestaurantId(restaurant),
                    lines, OrderStatus.valueOf(status), placedAt, history);
        } catch (IllegalArgumentException | OrderInvariantViolationException ex) {
            throw new InvalidPersistedOrderException(id, ex);
        }
    }

    static List<OrderLine> readLines(OrderId id, ResultSet rs) throws SQLException {
        var lines = new ArrayList<OrderLine>();
        int expectedPosition = 0;
        while (rs.next()) {
            var actualPosition = rs.getInt("line_position");
            requireExpectedPosition(id, "order_lines", expectedPosition, actualPosition);
            try {
                lines.add(new OrderLine(
                        rs.getString("menu_item_id"), rs.getString("item_name"),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_price")));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new InvalidPersistedOrderException(id, e);
            }
            expectedPosition++;
        }
        return List.copyOf(lines);
    }

    static List<OrderWorkflowOccurrence> readHistory(OrderId id, ResultSet rs)
            throws SQLException {
        var history = new ArrayList<OrderWorkflowOccurrence>();
        int expectedPosition = 0;
        while (rs.next()) {
            var actualPosition = rs.getInt("occurrence_position");
            requireExpectedPosition(
                    id, "order_workflow_occurrences", expectedPosition, actualPosition);
            try {
                history.add(new OrderWorkflowOccurrence(
                        OrderWorkflowAction.valueOf(rs.getString("action")),
                        WorkflowParticipant.valueOf(rs.getString("participant")),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new InvalidPersistedOrderException(id, e);
            }
            expectedPosition++;
        }
        return List.copyOf(history);
    }

    private static void requireExpectedPosition(
            OrderId id, String table, int expected, int actual) {
        if (actual != expected) {
            throw new InvalidPersistedOrderException(
                    id, new IllegalArgumentException(
                            table + " position is noncontiguous; expected "
                                    + expected + " but found " + actual));
        }
    }
}
