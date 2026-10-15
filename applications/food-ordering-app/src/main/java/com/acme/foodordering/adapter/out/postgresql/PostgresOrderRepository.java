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
 * P15: stores one immutable Order across the V1/V2 relational schema and
 * groups root/owned child writes in a single local JDBC transaction.
 * Distinguishes a pre-commit SQL error from a failed COMMIT acknowledgement.
 * Read-side P16 uses a read-only REPEATABLE READ snapshot.
 * P17 writes use aggregate version-checked conditional UPDATE.
 * P18 classifies recognized precommit SQL concurrency failures distinctly
 * from the uncertain COMMIT outcome established in P15.
 *
 * Connections are opened per operation: no pool or scaling claim at P12.
 */
public final class PostgresOrderRepository implements OrderRepository {
    private final String url;
    private final String username;
    private final String password;
    private final ConnectionSource connectionSource;
    private final RootReadObserver rootReadObserver;

    /** Test-only deterministic scheduling after first root SELECT; production is NOOP. */
    @FunctionalInterface
    interface RootReadObserver {
        void afterRootRead(OrderId id);
    }

    /**
     * A narrow package-private connection seam permits actual JDBC commit
     * acknowledgement failure injection without a fake database or changing
     * the public OrderRepository contract.
     */
    @FunctionalInterface
    interface ConnectionSource {
        Connection open() throws SQLException;
    }

    public PostgresOrderRepository(String url, String username, String password) {
        this(url, username, password,
                () -> DriverManager.getConnection(url, username, password),
                id -> {});
    }

    PostgresOrderRepository(
            String url, String username, String password, ConnectionSource connectionSource) {
        this(url, username, password, connectionSource, id -> {});
    }

    PostgresOrderRepository(
            String url, String username, String password, ConnectionSource connectionSource,
            RootReadObserver rootReadObserver) {
        this.url = requireNonBlank(url, "jdbc-url");
        this.username = requireNonBlank(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.connectionSource = Objects.requireNonNull(connectionSource);
        this.rootReadObserver = Objects.requireNonNull(rootReadObserver);
        // Fail startup rather than silently switching to in-memory authority.
        Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").load().migrate();
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " required");
        return value;
    }

    private Connection connection() throws SQLException {
        return connectionSource.open();
    }

    @Override
    public void createNew(Order order) {
        Objects.requireNonNull(order, "order");
        if (order.version() != 0L) throw new IllegalArgumentException("createNew expects version zero");
        try (var db = connection()) {
            db.setAutoCommit(false);
            boolean commitAttempted = false;
            try {
                insertRoot(db, order);
                insertLines(db, order);
                insertHistory(db, order);
                commitAttempted = true;
                db.commit();
            } catch (SQLException e) {
                rollback(db, e);
                if (commitAttempted) {
                    throw new OrderCommitOutcomeUnknownException("create Order", order.id(), e);
                }
                if ("23505".equals(e.getSQLState())) {
                    throw new OrderAlreadyExistsException(order.id());
                }
                throw beforeCommitFailure("create Order", order.id(), e);
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
            boolean commitAttempted = false;
            try {
                // Conditional root update is the linearization point for
                // aggregate currentness. A stale successor cannot reach
                // child DELETE or INSERT statements.
                if (order.version() < 1L)
                    throw new StaleOrderVersionException(order.id(), 0L);
                try (var ps = db.prepareStatement("""
                        UPDATE orders SET
                          customer_id=?,restaurant_id=?,status=?,placed_at=?,
                          version=?
                        WHERE id=? AND version=?
                        """)) {
                    ps.setString(1,order.customerId().value());
                    ps.setString(2,order.restaurantId().value());
                    ps.setString(3,order.status().name());
                    ps.setObject(4,OffsetDateTime.ofInstant(order.placedAt(),ZoneOffset.UTC));
                    ps.setLong(5,order.version());
                    ps.setObject(6,order.id().value());
                    ps.setLong(7,order.version()-1L);
                    if (ps.executeUpdate() != 1)
                        throw new StaleOrderVersionException(
                                order.id(),order.version()-1L);
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
                commitAttempted = true;
                db.commit();
            } catch (SQLException e) {
                rollback(db,e);
                if (commitAttempted) {
                    throw new OrderCommitOutcomeUnknownException("save Order", order.id(), e);
                }
                throw beforeCommitFailure("save Order",order.id(),e);
            } catch (RuntimeException e) {
                rollback(db,e);
                throw e;
            }
        } catch (SQLException e) {
            throw new OrderPersistenceException("save Order connection failed",e);
        }
    }

    /**
     * Only classify SQL errors known to occur BEFORE COMMIT. Never infer
     * rollback from a lost COMMIT acknowledgement (P15 remains authoritative).
     */
    private static OrderPersistenceException beforeCommitFailure(
            String operation, OrderId id, SQLException failure) {
        String sqlState = failure.getSQLState();
        if (sqlState == null) {
            return new OrderPersistenceException(operation+" failed before COMMIT",failure);
        }
        return switch (sqlState) {
            case "40001" -> new OrderDatabaseConcurrencyException(
                    operation,id,OrderDatabaseConcurrencyException.Kind.SERIALIZATION_FAILURE,failure);
            case "40P01" -> new OrderDatabaseConcurrencyException(
                    operation,id,OrderDatabaseConcurrencyException.Kind.DEADLOCK_DETECTED,failure);
            case "55P03" -> new OrderDatabaseConcurrencyException(
                    operation,id,OrderDatabaseConcurrencyException.Kind.LOCK_NOT_AVAILABLE,failure);
            default -> new OrderPersistenceException(
                    operation+" failed before COMMIT",failure);
        };
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
                "INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at,version) VALUES(?,?,?,?,?,?)")) {
            bindRoot(ps,o);
            ps.setLong(6,o.version());
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

    /**
     * One Order must be reconstituted from one committed snapshot, even though
     * its root and owned collections require three SELECT statements.
     * PostgreSQL READ COMMITTED takes a fresh snapshot per statement and can
     * produce a root/history combination that was never committed together.
     *
     * A read-only REPEATABLE READ transaction pins one MVCC snapshot for all
     * three SELECTs. It does NOT lock the Order, promise freshest state,
     * guard later writes, or provide SERIALIZABLE business decisions.
     */
    @Override
    public Optional<Order> findCurrentById(OrderId id) {
        Objects.requireNonNull(id,"id");
        try (var db=connection()) {
            // Connection settings must be established before executing SQL.
            db.setReadOnly(true);
            db.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            db.setAutoCommit(false);
            try {
                Optional<Order> result;
                try (var ps=db.prepareStatement(
                        "SELECT customer_id,restaurant_id,status,placed_at,version FROM orders WHERE id=?")) {
                    ps.setObject(1,id.value());
                    try (var rows=ps.executeQuery()) {
                        if(!rows.next()) {
                            result=Optional.empty();
                        } else {
                            String customer=rows.getString("customer_id");
                            String restaurant=rows.getString("restaurant_id");
                            String status=rows.getString("status");
                            Instant placedAt=rows.getObject("placed_at",OffsetDateTime.class).toInstant();
                            long version=rows.getLong("version");

                            // Deterministic test-only interleaving seam: the
                            // production observer is always NOOP.
                            rootReadObserver.afterRootRead(id);

                            List<OrderLine> lines=loadLines(db,id);
                            List<OrderWorkflowOccurrence> history=loadHistory(db,id);
                            result=Optional.of(PostgresOrderMapper.reconstitute(
                                    id,customer,restaurant,status,placedAt,lines,history,version));
                        }
                    }
                }
                db.commit(); // read-only; closes the transaction snapshot.
                return result;
            } catch (SQLException | RuntimeException e) {
                rollback(db,e);
                throw e;
            }
        } catch (SQLException e) {
            throw beforeCommitFailure("read Order",id,e);
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
