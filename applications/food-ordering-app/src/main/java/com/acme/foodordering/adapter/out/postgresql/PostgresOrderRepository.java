package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.*;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.*;
import org.flywaydb.core.Flyway;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * P12 durable Order state adapter. It stores one immutable Order across the
 * three V1 relational tables. Uses one local JDBC transaction per aggregate
 * write to prevent partial committed state; P15 investigates real transaction
 * failure windows in depth. saveCurrent deliberately remains LAST-WRITER-WINS
 * without a version/CAS condition (P16/P17 negative controls).
 *
 * Connections are opened per operation: no pool or scaling claim at P12.
 */
public final class PostgresOrderRepository implements OrderRepository {
    private final String url;
    private final String username;
    private final String password;

    public PostgresOrderRepository(String url, String username, String password) {
        this.url = requireNonBlank(url, "jdbc-url");
        this.username = requireNonBlank(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        // Fail startup rather than silently switching to in-memory authority.
        Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").load().migrate();
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " required");
        return value;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public void createNew(Order order) {
        Objects.requireNonNull(order, "order");
        try (var db = connection()) {
            db.setAutoCommit(false);
            try {
                insertRoot(db, order);
                insertLines(db, order);
                insertHistory(db, order);
                db.commit();
            } catch (SQLException e) {
                rollback(db, e);
                if ("23505".equals(e.getSQLState())) {
                    throw new OrderAlreadyExistsException(order.id());
                }
                throw new OrderPersistenceException("create Order failed", e);
            } catch (RuntimeException e) {
                rollback(db, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new OrderPersistenceException("create Order connection failed", e);
        }
    }

    @Override
    public void saveCurrent(Order order) {
        Objects.requireNonNull(order, "order");
        try (var db = connection()) {
            db.setAutoCommit(false);
            try {
                // Intentional P10-compatible upsert: no stale state/version guard.
                try (var ps = db.prepareStatement("""
                        INSERT INTO orders (id,customer_id,restaurant_id,status,placed_at)
                        VALUES (?,?,?,?,?)
                        ON CONFLICT (id) DO UPDATE SET
                          customer_id=EXCLUDED.customer_id,
                          restaurant_id=EXCLUDED.restaurant_id,
                          status=EXCLUDED.status,
                          placed_at=EXCLUDED.placed_at
                        """)) {
                    bindRoot(ps, order);
                    ps.executeUpdate();
                }
                try (var ps = db.prepareStatement(
                        "DELETE FROM order_workflow_occurrences WHERE order_id=?")) {
                    ps.setObject(1,order.id().value());ps.executeUpdate();
                }
                try (var ps = db.prepareStatement(
                        "DELETE FROM order_lines WHERE order_id=?")) {
                    ps.setObject(1,order.id().value());ps.executeUpdate();
                }
                insertLines(db, order);
                insertHistory(db, order);
                db.commit();
            } catch (SQLException e) {
                rollback(db,e);
                throw new OrderPersistenceException("save Order failed",e);
            } catch (RuntimeException e) {
                rollback(db,e);
                throw e;
            }
        } catch (SQLException e) {
            throw new OrderPersistenceException("save Order connection failed",e);
        }
    }

    private static void rollback(Connection db, Exception failure) {
        try {db.rollback();} catch (SQLException e) {failure.addSuppressed(e);}
    }

    private static void bindRoot(PreparedStatement ps, Order o) throws SQLException {
        ps.setObject(1,o.id().value());
        ps.setString(2,o.customerId().value());
        ps.setString(3,o.restaurantId().value());
        ps.setString(4,o.status().name());
        ps.setObject(5,OffsetDateTime.ofInstant(o.placedAt(),ZoneOffset.UTC));
    }

    private static void insertRoot(Connection db, Order o) throws SQLException {
        try (var ps=db.prepareStatement(
                "INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at) VALUES(?,?,?,?,?)")) {
            bindRoot(ps,o);
            ps.executeUpdate();
        }
    }

    private static void insertLines(Connection db,Order o) throws SQLException {
        try (var ps=db.prepareStatement(
                "INSERT INTO order_lines(order_id,line_position,menu_item_id,item_name,quantity,unit_price) VALUES(?,?,?,?,?,?)")) {
            for(int i=0;i<o.lines().size();i++){
                var line=o.lines().get(i);
                ps.setObject(1,o.id().value());
                ps.setInt(2,i);
                ps.setString(3,line.menuItemId());
                ps.setString(4,line.name());
                ps.setInt(5,line.quantity());
                ps.setBigDecimal(6,line.unitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertHistory(Connection db,Order o) throws SQLException {
        try (var ps=db.prepareStatement(
                "INSERT INTO order_workflow_occurrences(order_id,occurrence_position,action,participant,occurred_at) VALUES(?,?,?,?,?)")) {
            for(int i=0;i<o.workflowOccurrences().size();i++) {
                var w=o.workflowOccurrences().get(i);
                ps.setObject(1,o.id().value());
                ps.setInt(2,i);
                ps.setString(3,w.action().name());
                ps.setString(4,w.participant().name());
                ps.setObject(5,OffsetDateTime.ofInstant(w.occurredAt(),ZoneOffset.UTC));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public Optional<Order> findCurrentById(OrderId id) {
        Objects.requireNonNull(id,"id");
        try (var db=connection()) {
            try (var ps=db.prepareStatement(
                    "SELECT customer_id,restaurant_id,status,placed_at FROM orders WHERE id=?")) {
                ps.setObject(1,id.value());
                try (var rows=ps.executeQuery()) {
                    if(!rows.next()) return Optional.empty();
                    String customer=rows.getString("customer_id");
                    String restaurant=rows.getString("restaurant_id");
                    String status=rows.getString("status");
                    Instant placedAt=rows.getObject("placed_at",OffsetDateTime.class).toInstant();
                    // JDBC conversion and domain validation are separate
                    // responsibilities, owned by the persistence mapper.
                    List<OrderLine> lines = loadLines(db,id);
                    List<OrderWorkflowOccurrence> history = loadHistory(db,id);
                    return Optional.of(PostgresOrderMapper.reconstitute(
                            id, customer, restaurant, status, placedAt,
                            lines, history));
                }
            }
        } catch (SQLException e) {
            throw new OrderPersistenceException("read Order failed",e);
        }
    }


    private static List<OrderLine> loadLines(Connection db, OrderId id) throws SQLException {
        try (var ps=db.prepareStatement(
                "SELECT line_position,menu_item_id,item_name,quantity,unit_price "
                + "FROM order_lines WHERE order_id=? ORDER BY line_position")) {
            ps.setObject(1,id.value());
            try(var rs=ps.executeQuery()) {
                return PostgresOrderMapper.readLines(id,rs);
            }
        }
    }

    private static List<OrderWorkflowOccurrence> loadHistory(Connection db, OrderId id)
            throws SQLException {
        try (var ps=db.prepareStatement(
                "SELECT occurrence_position,action,participant,occurred_at "
                + "FROM order_workflow_occurrences WHERE order_id=? ORDER BY occurrence_position")) {
            ps.setObject(1,id.value());
            try(var rs=ps.executeQuery()) {
                return PostgresOrderMapper.readHistory(id,rs);
            }
        }
    }
}
