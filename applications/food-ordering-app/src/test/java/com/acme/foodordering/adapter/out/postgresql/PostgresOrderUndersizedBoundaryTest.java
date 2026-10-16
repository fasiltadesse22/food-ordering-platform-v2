package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.InvalidPersistedOrderException;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.OrderStatus;
import com.acme.foodordering.domain.order.RestaurantId;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * P20 real PostgreSQL NEGATIVE CONTROLS model what would happen if the local
 * Order status/history invariant were split into independent authorities.
 * Direct SQL in these tests deliberately BYPASSES the verified production
 * aggregate repository. Never copy it into production write code.
 *
 * Inventory tables are an explicitly test-only hypothetical independent
 * aggregate/authority example, NOT a production Inventory domain model,
 * payment provider, saga, or service boundary.
 */
class PostgresOrderUndersizedBoundaryTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174320");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p20");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p20");
    private static final Instant AT=Instant.parse("2026-10-16T07:00:00.123456Z");

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("ordering_p20")
                .withUsername("ordering")
                .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    @BeforeEach void reset() throws Exception {
        try(var db=connection();var st=db.createStatement()){
            st.execute("DROP TABLE IF EXISTS p20_reservations");
            st.execute("DROP TABLE IF EXISTS p20_inventory");
            st.execute("DROP FUNCTION IF EXISTS p20_reject_history() CASCADE");
            st.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
        }
    }
    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    private static Order source(){
        return Order.place(ID,CUSTOMER,RESTAURANT,
                List.of(new OrderLine("burger","Burger",1,new BigDecimal("5.00"))),AT);
    }
    private static void seed(){repo.createNew(source());}
    private static void statusCommit() throws SQLException {
        try(var db=connection();var ps=db.prepareStatement(
                "UPDATE orders SET status='ACCEPTED',version=version+1 WHERE id=?")){
            ps.setObject(1,ID.value());
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
    }
    private static void acceptanceHistoryCommit() throws SQLException {
        try(var db=connection()){
            insertAcceptance(db);
        }
    }
    private static void insertAcceptance(Connection db) throws SQLException {
        try(var ps=db.prepareStatement(
                "INSERT INTO order_workflow_occurrences("
                +"order_id,occurrence_position,action,participant,occurred_at)"
                +" VALUES(?,0,'RESTAURANT_ACCEPTED','RESTAURANT_OPERATOR',?)")){
            ps.setObject(1,ID.value());
            ps.setObject(2,OffsetDateTime.ofInstant(AT.plusSeconds(1),ZoneOffset.UTC));
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
    }
    private static String rawStatus() throws SQLException {
        try(var db=connection();var ps=db.prepareStatement(
                "SELECT status FROM orders WHERE id=?")){
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                return rs.getString(1);
            }
        }
    }
    private static long rawVersion() throws SQLException {
        try(var db=connection();var ps=db.prepareStatement(
                "SELECT version FROM orders WHERE id=?")){
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                return rs.getLong(1);
            }
        }
    }
    private static long acceptanceCount() throws SQLException{
        try(var db=connection();var ps=db.prepareStatement(
                "SELECT COUNT(*) FROM order_workflow_occurrences "
                +"WHERE order_id=? AND action='RESTAURANT_ACCEPTED'")){
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){rs.next();return rs.getLong(1);}
        }
    }

    @Test void committingStatusIndependentlyBreaksOrderHistoryInvariant() throws Exception{
        seed();
        statusCommit(); // test-only deliberately wrong separate authority
        assertThat(rawStatus()).isEqualTo("ACCEPTED");
        assertThat(rawVersion()).isEqualTo(1);
        assertThat(acceptanceCount()).isZero();
        assertThatThrownBy(()->repo.findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
        System.out.println("P20_SPLIT_STATUS_FIRST committed=ACCEPTED history=ABSENT domainReconstitution=REJECTED");
    }

    @Test void committingHistoryIndependentlyBreaksReverseOrderInvariant() throws Exception{
        seed();
        acceptanceHistoryCommit(); // test-only deliberately wrong separate authority
        assertThat(rawStatus()).isEqualTo("PLACED");
        assertThat(acceptanceCount()).isEqualTo(1);
        assertThatThrownBy(()->repo.findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
        System.out.println("P20_SPLIT_HISTORY_FIRST committed=RESTAURANT_ACCEPTED status=PLACED domainReconstitution=REJECTED");
    }

    @Test void aLaterFailedIndependentTransactionCannotUndoAnEarlierStatusCommit()
            throws Exception{
        seed();
        statusCommit(); // Already committed; future rollback cannot undo.
        try(var db=connection();var st=db.createStatement()){
            st.execute("CREATE FUNCTION p20_reject_history() RETURNS trigger "
                    +"LANGUAGE plpgsql AS $$ BEGIN "
                    +"RAISE EXCEPTION 'P20 injected history write failure' USING ERRCODE='P0001'; "
                    +"END; $$");
            st.execute("CREATE TRIGGER p20_fail_acceptance BEFORE INSERT "
                    +"ON order_workflow_occurrences FOR EACH ROW "
                    +"EXECUTE FUNCTION p20_reject_history()");
        }
        assertThatThrownBy(PostgresOrderUndersizedBoundaryTest::acceptanceHistoryCommit)
                .isInstanceOf(SQLException.class)
                .satisfies(ex->assertThat(((SQLException)ex).getSQLState()).isEqualTo("P0001"));
        assertThat(rawStatus()).isEqualTo("ACCEPTED");
        assertThat(rawVersion()).isEqualTo(1);
        assertThat(acceptanceCount()).isZero();
        assertThatThrownBy(()->repo.findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
        System.out.println("P20_SPLIT_FAILURE firstCommitSurvived=YES secondCommitFailed=YES crossInvariant=VIOLATED");
    }

    @Test void sameLocalTransactionKeepsExternalReadersOnOldOrNewValidOrder()
            throws Exception {
        seed();
        try(var writer=connection()){
            writer.setAutoCommit(false);
            try(var update=writer.prepareStatement(
                    "UPDATE orders SET status='ACCEPTED',version=version+1 WHERE id=?")){
                update.setObject(1,ID.value());
                assertThat(update.executeUpdate()).isEqualTo(1);
            }
            // Other connection cannot observe uncommitted ACCEPTED root.
            var before=repo.findCurrentById(ID).orElseThrow();
            assertThat(before.status()).isEqualTo(OrderStatus.PLACED);
            assertThat(before.workflowOccurrences()).isEmpty();
            assertThat(before.version()).isZero();
            insertAcceptance(writer);
            // One commit includes root and required explaining history.
            writer.commit();
        }
        var after=repo.findCurrentById(ID).orElseThrow();
        assertThat(after.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(after.version()).isEqualTo(1);
        assertThat(after.workflowOccurrences())
                .extracting(w->w.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        System.out.println("P20_SAME_TX externalBefore=PLACED_v0 externalAfter=ACCEPTED_v1 invariant=SATISFIED");
    }

    private static void setupInventory() throws SQLException{
        try(var db=connection();var st=db.createStatement()){
            st.execute("CREATE TABLE p20_inventory("
                    +"item_id TEXT PRIMARY KEY, capacity INT NOT NULL CHECK(capacity>=0),"
                    +"remaining INT NOT NULL CHECK(remaining>=0))");
            st.execute("CREATE TABLE p20_reservations("
                    +"reservation_id TEXT PRIMARY KEY,"
                    +"item_id TEXT NOT NULL REFERENCES p20_inventory(item_id),"
                    +"quantity INT NOT NULL CHECK(quantity>0))");
            st.execute("INSERT INTO p20_inventory VALUES('burger',1,1)");
        }
    }
    private static long reservations() throws SQLException {
        try(var db=connection();var st=db.createStatement();
                var rs=st.executeQuery("SELECT COALESCE(SUM(quantity),0) FROM p20_reservations")){
            assertThat(rs.next()).isTrue();return rs.getLong(1);
        }
    }
    private static void await(CountDownLatch latch) {
        try{
            if(!latch.await(15,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();throw new IllegalStateException(e);
        }
    }

    @Test void locallyValidIndependentReservationsCanOversellSharedInventory()
            throws Exception {
        setupInventory();
        // Two independent reservations each satisfy positive quantity and FK,
        // but no atomic authority enforces SUM(reservations)<=capacity.
        try(var db=connection();var ps=db.prepareStatement(
                "INSERT INTO p20_reservations VALUES(?,'burger',1)")){
            ps.setString(1,"order-A");assertThat(ps.executeUpdate()).isEqualTo(1);
            ps.setString(1,"order-B");assertThat(ps.executeUpdate()).isEqualTo(1);
        }
        assertThat(reservations()).isEqualTo(2);
        try(var db=connection();var st=db.createStatement();
                var rs=st.executeQuery("SELECT capacity,remaining FROM p20_inventory")){
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("capacity")).isEqualTo(1);
            assertThat(rs.getInt("remaining")).isEqualTo(1);
        }
        System.out.println("P20_TEST_ONLY_INVENTORY_SPLIT localReservationsValid=2 stockCapacity=1 globallyOversold=TRUE");
    }

    @Test void sharedInventoryAuthorityConditionallyAdmitsOnlyOneReservation()
            throws Exception{
        setupInventory();
        var ready=new CountDownLatch(2);
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->reserveConditionally("order-A",ready,start));
            var b=pool.submit(()->reserveConditionally("order-B",ready,start));
            await(ready);
            start.countDown();
            assertThat((a.get(15,TimeUnit.SECONDS)?1:0)
                    +(b.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        } finally{start.countDown();}
        assertThat(reservations()).isEqualTo(1);
        try(var db=connection();var st=db.createStatement();
                var rs=st.executeQuery("SELECT remaining FROM p20_inventory")){
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isZero();
        }
        System.out.println("P20_TEST_ONLY_INVENTORY_GUARDED admitted=1 rejected=1 remaining=0 crossInvariant=SATISFIED");
    }

    private static boolean reserveConditionally(
            String reservationId,CountDownLatch ready,CountDownLatch start) throws Exception {
        try(var db=connection()){
            db.setAutoCommit(false);
            try{
                // Both candidates obtain the same pre-decision view. Only the
                // conditional UPDATE below is authoritative admission.
                try(var ps=db.prepareStatement(
                        "SELECT remaining FROM p20_inventory WHERE item_id='burger'")){
                    try(var rs=ps.executeQuery()){
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).isEqualTo(1);
                    }
                }
                ready.countDown();await(start);
                int admitted;
                try(var ps=db.prepareStatement(
                        "UPDATE p20_inventory SET remaining=remaining-1 "
                        +"WHERE item_id='burger' AND remaining>=1")){
                    admitted=ps.executeUpdate();
                }
                if(admitted==0){db.rollback();return false;}
                try(var ps=db.prepareStatement(
                        "INSERT INTO p20_reservations VALUES(?,'burger',1)")){
                    ps.setString(1,reservationId);
                    assertThat(ps.executeUpdate()).isEqualTo(1);
                }
                db.commit();
                return true;
            } catch(Exception e){db.rollback();throw e;}
        }
    }
}
