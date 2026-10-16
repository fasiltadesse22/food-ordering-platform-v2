package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.domain.inventory.*;
import com.acme.foodordering.domain.order.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

/**
 * P21: REAL PostgreSQL local multi-aggregate transaction, failure-window
 * and stale planning experiments. Inventory tables and coordinated SQL are
 * TEST-ONLY, not a production feature, Payment provider or new service.
 */
class PostgresCrossAggregateCoordinationTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository orders;
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p21");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p21");
    private static final Instant AT=Instant.parse("2026-10-16T10:00:00Z");
    private static final OrderId A=OrderId.from("123e4567-e89b-12d3-a456-426614174322");
    private static final OrderId B=OrderId.from("123e4567-e89b-12d3-a456-426614174323");
    private static final OrderInventoryReservationPolicy POLICY=new OrderInventoryReservationPolicy();

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p21")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        orders=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    private static Connection connect() throws SQLException{
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @BeforeEach void schema() throws SQLException{
        try(var c=connect();var st=c.createStatement()){
            st.execute("DROP TABLE IF EXISTS p21_reservations CASCADE");
            st.execute("DROP TABLE IF EXISTS p21_stock CASCADE");
            st.execute("DROP FUNCTION IF EXISTS p21_fail_reservation() CASCADE");
            st.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
            st.execute("CREATE TABLE p21_stock (restaurant_id TEXT NOT NULL,"
                +"menu_item_id TEXT NOT NULL,remaining INT NOT NULL CHECK(remaining>=0),"
                +"PRIMARY KEY(restaurant_id,menu_item_id))");
            st.execute("CREATE TABLE p21_reservations (order_id UUID NOT NULL REFERENCES orders(id),"
                +"restaurant_id TEXT NOT NULL,menu_item_id TEXT NOT NULL,"
                +"quantity INT NOT NULL CHECK(quantity>0),"
                +"PRIMARY KEY(order_id,menu_item_id))");
            st.execute("INSERT INTO p21_stock VALUES('restaurant-p21','burger',1)");
        }
    }
    private static Order order(OrderId id){
        return Order.place(id,CUSTOMER,RESTAURANT,
                List.of(new OrderLine("burger","Burger",1,new BigDecimal("5.00"))),AT);
    }
    private static InventoryAvailabilitySnapshot observed() throws SQLException{
        try(var c=connect();var ps=c.prepareStatement(
                "SELECT remaining FROM p21_stock WHERE restaurant_id=? AND menu_item_id='burger'")){
            ps.setString(1,RESTAURANT.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                return new InventoryAvailabilitySnapshot(RESTAURANT,0,
                    Map.of("burger",rs.getInt(1)));
            }
        }
    }
    private static int remaining() throws SQLException {
        try(var c=connect();var s=c.createStatement();
                var rs=s.executeQuery("SELECT remaining FROM p21_stock")){
            assertThat(rs.next()).isTrue();return rs.getInt(1);
        }
    }
    private static int reservationCount() throws SQLException {
        try(var c=connect();var s=c.createStatement();
                var rs=s.executeQuery("SELECT COUNT(*) FROM p21_reservations")){
            assertThat(rs.next()).isTrue();return rs.getInt(1);
        }
    }
    private static void insertOrder(Connection c,Order order) throws SQLException {
        try(var ps=c.prepareStatement(
                "INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at,version)"
                +" VALUES(?,?,?,?,?,?)")){
            ps.setObject(1,order.id().value());
            ps.setString(2,order.customerId().value());
            ps.setString(3,order.restaurantId().value());
            ps.setString(4,order.status().name());
            ps.setObject(5,OffsetDateTime.ofInstant(order.placedAt(),ZoneOffset.UTC));
            ps.setLong(6,order.version());
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
        for(int i=0;i<order.lines().size();i++){
            var line=order.lines().get(i);
            try(var ps=c.prepareStatement(
                    "INSERT INTO order_lines(order_id,line_position,menu_item_id,item_name,quantity,unit_price)"
                    +" VALUES(?,?,?,?,?,?)")){
                ps.setObject(1,order.id().value());ps.setInt(2,i);
                ps.setString(3,line.menuItemId());ps.setString(4,line.name());
                ps.setInt(5,line.quantity());ps.setBigDecimal(6,line.unitPrice());
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
        }
    }
    /**
     * Demonstration-only same-DB coordinator: NOT a shipped application
     * service. Orders and Inventory remain separate conceptual authorities.
     * The complete SQL admission and Order creation COMMIT together.
     */
    private static boolean atomicCreateAndAllocate(
            Order order, InventoryReservationPlan plan) throws SQLException {
        try(var db=connect()){
            db.setAutoCommit(false);
            try {
                insertOrder(db,order);
                for(var entry:plan.requestedByMenuItem().entrySet()){
                    try(var ps=db.prepareStatement(
                            "UPDATE p21_stock SET remaining=remaining-?"
                            +" WHERE restaurant_id=? AND menu_item_id=? AND remaining>=?")){
                        ps.setInt(1,entry.getValue());
                        ps.setString(2,plan.restaurantId().value());
                        ps.setString(3,entry.getKey());
                        ps.setInt(4,entry.getValue());
                        if(ps.executeUpdate()!=1){db.rollback();return false;}
                    }
                    try(var ps=db.prepareStatement(
                            "INSERT INTO p21_reservations VALUES(?,?,?,?)")){
                        ps.setObject(1,order.id().value());
                        ps.setString(2,plan.restaurantId().value());
                        ps.setString(3,entry.getKey());
                        ps.setInt(4,entry.getValue());
                        assertThat(ps.executeUpdate()).isEqualTo(1);
                    }
                }
                db.commit();
                return true;
            }catch(SQLException|RuntimeException e){db.rollback();throw e;}
        }
    }

    @Test void sameLocalTransactionPublishesOrderAndSharedReservationTogether()
            throws Exception{
        var candidate=order(A);
        var plan=POLICY.propose(candidate,observed());
        assertThat(atomicCreateAndAllocate(candidate,plan)).isTrue();
        assertThat(orders.findCurrentById(A).orElseThrow().lines()).isEqualTo(candidate.lines());
        assertThat(remaining()).isZero();
        assertThat(reservationCount()).isEqualTo(1);
        System.out.println("P21_SINGLE_LOCAL_TX orderPresent=YES reservation=1 remaining=0");
    }

    @Test void independentOrderCommitCanSurviveFailedStockReservation()
            throws Exception{
        var candidate=order(A);
        var plan=POLICY.propose(candidate,observed()); // observed remaining1
        orders.createNew(candidate); // first independent COMMIT
        try(var db=connect();var st=db.createStatement()){
            st.execute("UPDATE p21_stock SET remaining=0"); // another allocation/adjustment
        }
        assertThat(atomicReserveExistingOrder(plan)).isFalse();
        assertThat(orders.findCurrentById(A)).isPresent();
        assertThat(reservationCount()).isZero();
        assertThat(remaining()).isZero();
        System.out.println("P21_ORDER_FIRST_PARTIAL orderCommitted=YES reservation=ABSENT");
    }
    private static boolean atomicReserveExistingOrder(InventoryReservationPlan plan)
            throws SQLException{
        try(var db=connect()){
            db.setAutoCommit(false);
            try{
                for(var entry:plan.requestedByMenuItem().entrySet()){
                    try(var ps=db.prepareStatement(
                            "UPDATE p21_stock SET remaining=remaining-?"
                            +" WHERE restaurant_id=? AND menu_item_id=? AND remaining>=?")){
                        ps.setInt(1,entry.getValue());ps.setString(2,plan.restaurantId().value());
                        ps.setString(3,entry.getKey());ps.setInt(4,entry.getValue());
                        if(ps.executeUpdate()!=1){db.rollback();return false;}
                    }
                    try(var ps=db.prepareStatement("INSERT INTO p21_reservations VALUES(?,?,?,?)")){
                        ps.setObject(1,plan.orderId().value());ps.setString(2,plan.restaurantId().value());
                        ps.setString(3,entry.getKey());ps.setInt(4,entry.getValue());
                        assertThat(ps.executeUpdate()).isEqualTo(1);
                    }
                }
                db.commit();return true;
            }catch(SQLException|RuntimeException e){db.rollback();throw e;}
        }
    }

    @Test void independentStockCommitWithoutOrderCreationLeavesLostCapacity()
            throws Exception{
        var order=order(A);
        var plan=POLICY.propose(order,observed());
        try(var db=connect();var ps=db.prepareStatement(
                "UPDATE p21_stock SET remaining=remaining-1"
                +" WHERE restaurant_id=? AND menu_item_id='burger' AND remaining>=1")){
            ps.setString(1,RESTAURANT.value());
            assertThat(ps.executeUpdate()).isEqualTo(1);
            // Autocommit=true; crash or failure before Order creation
        }
        assertThat(orders.findCurrentById(A)).isEmpty();
        assertThat(remaining()).isZero();
        assertThat(reservationCount()).isZero();
        assertThat(plan.requestedByMenuItem()).containsEntry("burger",1);
        System.out.println("P21_STOCK_FIRST_PARTIAL stockCommitted=YES orderAbsent=YES");
    }

    @Test void laterSqlFailureRollsBackBothLocalAuthoritiesInOneTransaction()
            throws Exception{
        var candidate=order(A);
        var plan=POLICY.propose(candidate,observed());
        try(var c=connect();var st=c.createStatement()){
            st.execute("CREATE FUNCTION p21_fail_reservation() RETURNS trigger "
                +"LANGUAGE plpgsql AS $$ BEGIN "
                +"RAISE EXCEPTION 'P21 injected second-authority SQL failure'"
                +" USING ERRCODE='P0001'; END; $$");
            st.execute("CREATE TRIGGER p21_fail_allocation BEFORE INSERT ON p21_reservations "
                +"FOR EACH ROW EXECUTE FUNCTION p21_fail_reservation()");
        }
        assertThatThrownBy(()->atomicCreateAndAllocate(candidate,plan))
            .isInstanceOfSatisfying(SQLException.class,
                e->assertThat(e.getSQLState()).isEqualTo("P0001"));
        assertThat(orders.findCurrentById(A)).isEmpty();
        assertThat(remaining()).isEqualTo(1);
        assertThat(reservationCount()).isZero();
        System.out.println("P21_SHARED_TX_ROLLBACK orderAbsent=YES stockRestored=YES reservationAbsent=YES");
    }

    @Test void racingPlansFromSameSnapshotCannotBothPassAuthoritativeAdmission()
            throws Exception{
        var one=order(A);var two=order(B);
        var sameSnapshot=observed();
        var p1=POLICY.propose(one,sameSnapshot);
        var p2=POLICY.propose(two,sameSnapshot);
        var ready=new CountDownLatch(2);var release=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var f1=pool.submit(()->{ready.countDown();await(release);
                return atomicCreateAndAllocate(one,p1);});
            var f2=pool.submit(()->{ready.countDown();await(release);
                return atomicCreateAndAllocate(two,p2);});
            await(ready);release.countDown();
            boolean r1=f1.get(15,TimeUnit.SECONDS);
            boolean r2=f2.get(15,TimeUnit.SECONDS);
            assertThat(r1!=r2).isTrue();
            assertThat(orders.findCurrentById(A).isPresent()).isEqualTo(r1);
            assertThat(orders.findCurrentById(B).isPresent()).isEqualTo(r2);
        }finally{release.countDown();}
        assertThat(remaining()).isZero();
        assertThat(reservationCount()).isEqualTo(1);
        System.out.println("P21_RACING_PLANS snapshotsBothEligible=YES admitted=1 rejected=1 remaining=0");
    }
    private static void await(CountDownLatch latch){
        try{if(!latch.await(15,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
}
