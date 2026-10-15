package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * P16: deterministic PostgreSQL 16 isolation/locking experiments.
 * The P15 write transaction remains intact. The read-only snapshot fix is
 * tested against an actual concurrent writer, NOT a mocked SQL response.
 */
class PostgresOrderIsolationTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174261");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p16");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p16");
    private static final Instant AT=Instant.parse("2026-10-15T14:00:00.123456Z");

    @BeforeAll static void start() {
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p16").withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(
            pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }

    @AfterAll static void stop() {if(pg!=null)pg.stop();}

    @BeforeEach void clean() throws SQLException {
        try(var db=connection();var s=db.createStatement()) {
            s.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }

    private static Order placed() {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",2,new BigDecimal("5.50")),
            new OrderLine("water","Water",1,new BigDecimal("1.25"))),AT);
    }

    private static Order accepted(Order base) {
        return base.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
    }

    private static String status(Connection db) throws SQLException {
        try(var ps=db.prepareStatement("SELECT status FROM orders WHERE id=?")) {
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                return rs.getString(1);
            }
        }
    }

    private static List<String> actions(Connection db) throws SQLException {
        var values=new java.util.ArrayList<String>();
        try(var ps=db.prepareStatement(
                "SELECT action FROM order_workflow_occurrences "
              + "WHERE order_id=? ORDER BY occurrence_position")) {
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()) {
                while(rs.next()) values.add(rs.getString(1));
            }
        }
        return values;
    }

    private static void waitFor(CountDownLatch latch, String label) {
        try {
            if(!latch.await(12,TimeUnit.SECONDS))
                throw new IllegalStateException("P16 timeout waiting for "+label);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("P16 interrupted waiting for "+label,e);
        }
    }

    @Test void readCommittedCanAssembleRootAndHistoryFromDifferentCommits()
            throws Exception {
        var original=placed();repo.createNew(original);
        try(var reader=connection()){
            reader.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            reader.setAutoCommit(false);
            var oldStatus=status(reader);
            assertThat(oldStatus).isEqualTo("PLACED");
            repo.saveCurrent(accepted(original)); // distinct real DB session
            var newActions=actions(reader);
            assertThat(newActions).containsExactly("RESTAURANT_ACCEPTED");
            assertThatThrownBy(()->Order.reconstitute(
                ID,CUSTOMER,RESTAURANT,original.lines(),
                OrderStatus.valueOf(oldStatus),AT,
                accepted(original).workflowOccurrences()))
                .isInstanceOf(OrderInvariantViolationException.class);
            reader.commit();
        }
        // There was never a committed PLACED+ACCEPTANCE aggregate.
        assertThat(repo.findCurrentById(ID).orElseThrow().status())
            .isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test void readCommittedAllowsNonrepeatableStatusWithinOneTransaction()
            throws Exception {
        var original=placed();repo.createNew(original);
        try(var reader=connection()){
            reader.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            reader.setAutoCommit(false);
            assertThat(status(reader)).isEqualTo("PLACED");
            repo.saveCurrent(accepted(original));
            assertThat(status(reader)).isEqualTo("ACCEPTED");
            reader.commit();
        }
    }

    @Test void repeatableReadPreservesRootAndHistorySnapshotAcrossCommit()
            throws Exception {
        var original=placed();repo.createNew(original);
        try(var reader=connection()){
            reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            reader.setAutoCommit(false);
            assertThat(status(reader)).isEqualTo("PLACED"); // fixes snapshot
            repo.saveCurrent(accepted(original));
            assertThat(status(reader)).isEqualTo("PLACED");
            assertThat(actions(reader)).isEmpty();
            reader.commit();
        }
        assertThat(repo.findCurrentById(ID).orElseThrow().status())
            .isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test void productionRepositoryReadsOneSnapshotWithCommitBetweenSelects()
            throws Exception {
        var original=placed();repo.createNew(original);
        var afterRoot=new CountDownLatch(1);
        var resume=new CountDownLatch(1);
        var pausedReader=new PostgresOrderRepository(
            pg.getJdbcUrl(),pg.getUsername(),pg.getPassword(),
            PostgresOrderIsolationTest::connection,
            id->{afterRoot.countDown();waitFor(resume,"reader release");});
        try(var pool=Executors.newSingleThreadExecutor()){
            var pending=pool.submit(()->pausedReader.findCurrentById(ID).orElseThrow());
            waitFor(afterRoot,"root SELECT");
            // Writer commits WHILE read-only transaction is between its
            // root SELECT and owned child SELECTs; reader does NOT block writer.
            repo.saveCurrent(accepted(original));
            resume.countDown();
            var snapshot=pending.get(12,TimeUnit.SECONDS);
            assertThat(snapshot.status()).isEqualTo(OrderStatus.PLACED);
            assertThat(snapshot.workflowOccurrences()).isEmpty();
            assertThat(snapshot.lines()).isEqualTo(original.lines());
        } finally {resume.countDown();}
        var latest=repo.findCurrentById(ID).orElseThrow();
        assertThat(latest.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(latest.workflowOccurrences())
            .extracting(w->w.action())
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void absentLookupStillReturnsEmptyWithinReadOnlySnapshot()
            throws Exception {
        assertThat(repo.findCurrentById(ID)).isEmpty();
    }

    @Test void aHeldWriteRowLockCausesBoundedSecondWriterLockTimeout()
            throws Exception {
        repo.createNew(placed());
        try(var first=connection();var second=connection()) {
            first.setAutoCommit(false);
            try(var ps=first.prepareStatement(
                    "UPDATE orders SET status='PLACED' WHERE id=?")){
                ps.setObject(1,ID.value());
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
            try(var st=second.createStatement()){
                st.execute("SET lock_timeout = '250ms'");
            }
            try(var ps=second.prepareStatement(
                    "UPDATE orders SET status='PLACED' WHERE id=?")){
                ps.setObject(1,ID.value());
                assertThatThrownBy(ps::executeUpdate)
                    .isInstanceOf(SQLException.class)
                    .satisfies(e->assertThat(((SQLException)e).getSQLState())
                        .isEqualTo("55P03"));
            }
            first.rollback();
        }
        assertThat(repo.findCurrentById(ID).orElseThrow().status())
            .isEqualTo(OrderStatus.PLACED);
    }

    @Test void selectForUpdateNowaitConflictsButOrdinaryMvccReadDoesNot()
            throws Exception {
        repo.createNew(placed());
        try(var first=connection();var second=connection()) {
            first.setAutoCommit(false);
            try(var ps=first.prepareStatement(
                    "SELECT status FROM orders WHERE id=? FOR UPDATE")){
                ps.setObject(1,ID.value());
                try(var rs=ps.executeQuery()){
                    assertThat(rs.next()).isTrue();
                }
            }
            assertThat(status(second)).isEqualTo("PLACED");
            try(var ps=second.prepareStatement(
                    "SELECT status FROM orders WHERE id=? FOR UPDATE NOWAIT")){
                ps.setObject(1,ID.value());
                assertThatThrownBy(ps::executeQuery)
                    .isInstanceOf(SQLException.class)
                    .satisfies(e->assertThat(((SQLException)e).getSQLState())
                        .isEqualTo("55P03"));
            }
            first.rollback();
        }
    }

    @Test void concurrentDomainDecisionsFromSameSnapshotNowYieldOneConflict()
            throws Exception {
        repo.createNew(placed());
        var bothRead=new CountDownLatch(2);
        var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var accepting=pool.submit(()->{
                var source=repo.findCurrentById(ID).orElseThrow();
                bothRead.countDown();waitFor(go,"release acceptance");
                try {
                    repo.saveCurrent(source.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1)));
                    return true;
                } catch(com.acme.foodordering.application.port.out.StaleOrderVersionException conflict) {
                    return false;
                }
            });
            var rejecting=pool.submit(()->{
                var source=repo.findCurrentById(ID).orElseThrow();
                bothRead.countDown();waitFor(go,"release rejection");
                try {
                    repo.saveCurrent(source.recordRestaurantRejection(RESTAURANT,AT.plusSeconds(2)));
                    return true;
                } catch(com.acme.foodordering.application.port.out.StaleOrderVersionException conflict) {
                    return false;
                }
            });
            waitFor(bothRead,"both reads");
            go.countDown();
            boolean a=accepting.get(12,TimeUnit.SECONDS);
            boolean b=rejecting.get(12,TimeUnit.SECONDS);
            assertThat(a != b).isTrue();
        } finally {go.countDown();}
        var finalOrder=repo.findCurrentById(ID).orElseThrow();
        assertThat(finalOrder.status()).isIn(OrderStatus.ACCEPTED,OrderStatus.REJECTED);
        assertThat(finalOrder.workflowOccurrences()).hasSize(1);
        // P16 checkpoint preserves both-accepted negative control.
        // P17 CAS guarantees exactly one currentness winner.
        assertThat(finalOrder.workflowOccurrences().get(0).action()).isIn(
            OrderWorkflowAction.RESTAURANT_ACCEPTED,
            OrderWorkflowAction.RESTAURANT_REJECTED);
    }

    @Test void repeatableReadWriteConflictRaisesSerializationFailure40001()
            throws Exception {
        var original=placed();repo.createNew(original);
        try(var staleTx=connection()) {
            staleTx.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            staleTx.setAutoCommit(false);
            assertThat(status(staleTx)).isEqualTo("PLACED");
            repo.saveCurrent(accepted(original));
            try(var ps=staleTx.prepareStatement(
                    "UPDATE orders SET status='REJECTED' WHERE id=?")){
                ps.setObject(1,ID.value());
                assertThatThrownBy(ps::executeUpdate)
                    .isInstanceOf(SQLException.class)
                    .satisfies(e->assertThat(((SQLException)e).getSQLState())
                        .isEqualTo("40001"));
            }
            staleTx.rollback();
        }
        assertThat(repo.findCurrentById(ID).orElseThrow().status())
            .isEqualTo(OrderStatus.ACCEPTED);
        // The repository's read and save are SEPARATE transactions, so this
        // PG protection alone does not solve its existing stale-write problem.
    }
}
