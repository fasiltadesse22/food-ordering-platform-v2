package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.InvalidPersistedOrderException;
import com.acme.foodordering.application.port.out.OrderAlreadyExistsException;
import com.acme.foodordering.application.port.out.OrderCommitOutcomeUnknownException;
import com.acme.foodordering.application.port.out.OrderPersistenceException;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.OrderStatus;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * P15: real PostgreSQL rollback and COMMIT outcome experiments. Except for
 * the explicitly labeled post-commit acknowledgement proxy, failure injection
 * occurs inside the actual database using PL/pgSQL triggers/constraint triggers.
 * Not a simulated repository or a fake SQL engine.
 */
class PostgresOrderTransactionBoundaryTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174251");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p15");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p15");
    private static final Instant AT=Instant.parse("2026-10-15T07:12:13.123456Z");

    @BeforeAll static void start() {
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p15")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }

    @AfterAll static void stop() {if(pg!=null)pg.stop();}

    @BeforeEach void reset() throws SQLException {
        try (var db=connection();var stmt=db.createStatement()) {
            // This is test-only database fault injection. Never migrate these
            // functions into the application's production SQL resources.
            stmt.execute("DROP FUNCTION IF EXISTS p15_injected_failure() CASCADE");
            stmt.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }

    private static void sql(String statement) throws SQLException {
        try(var db=connection();var s=db.createStatement()) {s.execute(statement);}
    }

    private static Order initial() {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",2,new BigDecimal("5.50")),
            new OrderLine("water","Water",1,new BigDecimal("1.25"))),AT);
    }

    private static Order accepted(Order original) {
        return original.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
    }

    private static void failureFunction() throws SQLException {
        sql("CREATE FUNCTION p15_injected_failure() RETURNS trigger "
          + "LANGUAGE plpgsql AS $$ "
          + "BEGIN RAISE EXCEPTION 'P15 controlled database failure' "
          + "USING ERRCODE='P0001'; END; $$");
    }

    private static void rejectInsertOn(String table) throws SQLException {
        failureFunction();
        sql("CREATE TRIGGER p15_reject BEFORE INSERT ON "+table
            +" FOR EACH ROW EXECUTE FUNCTION p15_injected_failure()");
    }

    private static void rejectDeferredAtCommit() throws SQLException {
        failureFunction();
        sql("CREATE CONSTRAINT TRIGGER p15_fail_at_commit "
            +"AFTER INSERT ON order_workflow_occurrences "
            +"DEFERRABLE INITIALLY DEFERRED "
            +"FOR EACH ROW EXECUTE FUNCTION p15_injected_failure()");
    }

    private static long count(String table) throws SQLException {
        try(var db=connection();var s=db.createStatement();
            var rs=s.executeQuery("SELECT COUNT(*) FROM "+table)) {
            assertThat(rs.next()).isTrue();
            return rs.getLong(1);
        }
    }

    private static void assertOriginalStateSurvives() throws SQLException {
        var recovered=repo.findCurrentById(ID).orElseThrow();
        assertThat(recovered.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(recovered.lines()).isEqualTo(initial().lines());
        assertThat(recovered.workflowOccurrences()).isEmpty();
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_lines")).isEqualTo(2);
        assertThat(count("order_workflow_occurrences")).isZero();
    }

    @Test void rootInsertRollsBackIfLaterLineInsertFailsInRealPostgres() throws Exception {
        rejectInsertOn("order_lines");
        assertThatThrownBy(()->repo.createNew(initial()))
            .isInstanceOf(OrderPersistenceException.class)
            .isNotInstanceOf(OrderCommitOutcomeUnknownException.class)
            .hasRootCauseInstanceOf(SQLException.class);
        assertThat(repo.findCurrentById(ID)).isEmpty();
        assertThat(count("orders")).isZero();
        assertThat(count("order_lines")).isZero();
        assertThat(count("order_workflow_occurrences")).isZero();
    }

    @Test void updatingRootAndDeletingCollectionsRollBackIfReplacementLineFails()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        rejectInsertOn("order_lines");
        assertThatThrownBy(()->repo.saveCurrent(accepted(old)))
            .isInstanceOf(OrderPersistenceException.class)
            .isNotInstanceOf(OrderCommitOutcomeUnknownException.class);
        assertOriginalStateSurvives();
    }

    @Test void rootAndReinsertedLinesRollBackIfLaterHistoryInsertFails()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        rejectInsertOn("order_workflow_occurrences");
        assertThatThrownBy(()->repo.saveCurrent(accepted(old)))
            .isInstanceOf(OrderPersistenceException.class)
            .isNotInstanceOf(OrderCommitOutcomeUnknownException.class);
        assertOriginalStateSurvives();
    }

    @Test void deferredConstraintFailureAtCommitRollsBackEveryPriorDml()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        rejectDeferredAtCommit();
        assertThatThrownBy(()->repo.saveCurrent(accepted(old)))
            .isInstanceOf(OrderCommitOutcomeUnknownException.class)
            .hasRootCauseInstanceOf(SQLException.class);
        // Though the repository conservatively reports uncertain COMMIT, the
        // independent observer in THIS controlled case proves DB rolled back.
        assertOriginalStateSurvives();
    }

    @Test void uncommittedChangesAreInvisibleUntilCommitThenBecomeAuthoritative()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        try(var writer=connection()) {
            writer.setAutoCommit(false);
            try(var root=writer.prepareStatement(
                    "UPDATE orders SET status='ACCEPTED' WHERE id=?")) {
                root.setObject(1,ID.value());root.executeUpdate();
            }
            try(var history=writer.prepareStatement(
                    "INSERT INTO order_workflow_occurrences("
                    +"order_id,occurrence_position,action,participant,occurred_at) "
                    +"VALUES(?,0,'RESTAURANT_ACCEPTED','RESTAURANT_OPERATOR',?)")) {
                history.setObject(1,ID.value());
                history.setObject(2,java.time.OffsetDateTime.ofInstant(
                    AT.plusSeconds(1),java.time.ZoneOffset.UTC));
                history.executeUpdate();
            }
            // Independent DB connection cannot see uncommitted writer state.
            assertOriginalStateSurvives();
            writer.commit();
        }
        var latest=repo.findCurrentById(ID).orElseThrow();
        assertThat(latest.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(latest.workflowOccurrences())
            .extracting(OrderWorkflowOccurrence::action)
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void terminatingRealUncommittedPostgresBackendLosesEntireTransaction()
            throws Exception {
        repo.createNew(initial());
        try(var writer=connection()) {
            writer.setAutoCommit(false);
            int backendPid;
            try(var s=writer.createStatement();var rs=s.executeQuery("SELECT pg_backend_pid()")) {
                rs.next();backendPid=rs.getInt(1);
            }
            try(var ps=writer.prepareStatement(
                    "UPDATE orders SET status='ACCEPTED' WHERE id=?")) {
                ps.setObject(1,ID.value());ps.executeUpdate();
            }
            try(var ps=writer.prepareStatement(
                    "INSERT INTO order_workflow_occurrences("
                    +"order_id,occurrence_position,action,participant,occurred_at) "
                    +"VALUES(?,0,'RESTAURANT_ACCEPTED','RESTAURANT_OPERATOR',?)")) {
                ps.setObject(1,ID.value());
                ps.setObject(2,java.time.OffsetDateTime.ofInstant(
                    AT.plusSeconds(1),java.time.ZoneOffset.UTC));
                ps.executeUpdate();
            }
            assertOriginalStateSurvives();
            try(var independent=connection();
                var ps=independent.prepareStatement("SELECT pg_terminate_backend(?)")) {
                ps.setInt(1,backendPid);
                try(var rs=ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBoolean(1)).isTrue();
                }
            }
            // No COMMIT was issued by writer. Actual PostgreSQL backend
            // termination aborts its open uncommitted transaction.
        } catch(SQLException maybeClosedDuringCleanup) {
            // The only permitted cleanup SQL error is an already terminated
            // connection; postcondition below is the decisive evidence.
            if(!"08006".equals(maybeClosedDuringCleanup.getSQLState())
                && !"08003".equals(maybeClosedDuringCleanup.getSQLState()))
                throw maybeClosedDuringCleanup;
        }
        assertOriginalStateSurvives();
    }

    @Test void autocommitSeparateStatementsExposeTheOriginalSplitStateFailure()
            throws Exception {
        repo.createNew(initial());
        sql("UPDATE orders SET status='ACCEPTED' WHERE id='"
            +ID+"'");
        // A row-level valid status change was committed alone; no history.
        assertThatThrownBy(()->repo.findCurrentById(ID))
            .isInstanceOf(InvalidPersistedOrderException.class);
        assertThat(count("order_workflow_occurrences")).isZero();
        // This is a test-only fragile representation; @BeforeEach resets it.
    }

    private static PostgresOrderRepository acknowledgeLossAfterServerCommit() {
        return new PostgresOrderRepository(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword(),()-> {
                    Connection db=connection();
                    return (Connection)Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy,method,args)-> {
                            if("commit".equals(method.getName())) {
                                db.commit();
                                // Deterministic stand-in for COMMIT success on
                                // server followed by lost acknowledgement.
                                throw new SQLException(
                                    "P15 injected lost commit acknowledgement", "08006");
                            }
                            try {
                                return method.invoke(db,args);
                            } catch(InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
                });
    }

    @Test void lostAcknowledgementAfterCommittedCreationIsNotKnownRollback()
            throws Exception {
        var faulted=acknowledgeLossAfterServerCommit();
        assertThatThrownBy(()->faulted.createNew(initial()))
            .isInstanceOf(OrderCommitOutcomeUnknownException.class)
            .satisfies(e-> assertThat(
                ((OrderCommitOutcomeUnknownException)e).orderId()).isEqualTo(ID));
        // A new independent connection establishes what actually happened
        // in THIS controlled injection; the caller at failure time cannot.
        var found=repo.findCurrentById(ID).orElseThrow();
        assertThat(found.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.lines()).isEqualTo(initial().lines());
        assertThatThrownBy(()->repo.createNew(initial()))
            .isInstanceOf(OrderAlreadyExistsException.class);
    }

    @Test void lostAcknowledgementAfterCommittedSuccessorIsNotKnownRollback()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        var faulted=acknowledgeLossAfterServerCommit();
        assertThatThrownBy(()->faulted.saveCurrent(accepted(old)))
            .isInstanceOf(OrderCommitOutcomeUnknownException.class);
        var found=repo.findCurrentById(ID).orElseThrow();
        assertThat(found.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(found.workflowOccurrences())
            .extracting(OrderWorkflowOccurrence::action)
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void successfulAggregateCommitMakesRootLinesAndHistoryVisibleTogether()
            throws Exception {
        var old=initial();
        repo.createNew(old);
        repo.saveCurrent(accepted(old));
        var second=new PostgresOrderRepository(
            pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
        var recovered=second.findCurrentById(ID).orElseThrow();
        assertThat(recovered.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(recovered.lines()).isEqualTo(old.lines());
        assertThat(recovered.workflowOccurrences())
            .extracting(OrderWorkflowOccurrence::action)
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_lines")).isEqualTo(2);
        assertThat(count("order_workflow_occurrences")).isEqualTo(1);
    }
}
