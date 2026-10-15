package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.in.ModifyOrderCommand;
import com.acme.foodordering.application.port.out.OrderCommitOutcomeUnknownException;
import com.acme.foodordering.application.port.out.OrderDatabaseConcurrencyException;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.port.out.StaleOrderVersionException;
import com.acme.foodordering.application.service.ModifyOrderService;
import com.acme.foodordering.application.service.OrderWorkflowService;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * C1.2-P18: actual PostgreSQL state and SQLState outcomes, with deterministic
 * wrapper injection at the application/repository decision boundary.
 * Simulated lost COMMIT acknowledgement is explicitly labeled; the underlying
 * COMMIT really executes against PostgreSQL.
 */
class PostgresOrderConflictResolutionTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174281");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p18");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p18");
    private static final Instant AT=Instant.parse("2026-10-15T16:00:00.123456Z");
    private static final Clock CLOCK=Clock.fixed(AT.plusSeconds(30),ZoneOffset.UTC);

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p18")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    @BeforeEach void clean() throws SQLException{
        try(var c=connection();var st=c.createStatement()){
            st.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
        }
    }
    private static Connection connection() throws SQLException{
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    private static Order placed() {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",2,new BigDecimal("5.50"))),AT);
    }
    private static Order saved(){
        var order=placed();repo.createNew(order);return order;
    }
    private static long version(){return repo.findCurrentById(ID).orElseThrow().version();}
    private static List<OrderWorkflowAction> actions(){
        return repo.findCurrentById(ID).orElseThrow().workflowOccurrences()
            .stream().map(w->w.action()).toList();
    }
    private static void rejected(OrderActionResult result,OrderActionRejection.Code code){
        assertThat(result).isInstanceOfSatisfying(OrderActionResult.Rejected.class,
            rejected->assertThat(rejected.rejection().code()).isEqualTo(code));
    }

    private static final class InterleavedRepository implements OrderRepository {
        private final AtomicInteger reads=new AtomicInteger();
        private final AtomicInteger saves=new AtomicInteger();
        private final java.util.function.Consumer<Order> concurrentCommit;
        private final int injectOnSaves;
        InterleavedRepository(int injectOnSaves,java.util.function.Consumer<Order> concurrentCommit){
            this.injectOnSaves=injectOnSaves;
            this.concurrentCommit=concurrentCommit;
        }
        @Override public void createNew(Order o){repo.createNew(o);}
        @Override public Optional<Order> findCurrentById(OrderId id){
            reads.incrementAndGet();return repo.findCurrentById(id);
        }
        @Override public void saveCurrent(Order candidate){
            if(saves.incrementAndGet()<=injectOnSaves)
                concurrentCommit.accept(repo.findCurrentById(candidate.id()).orElseThrow());
            repo.saveCurrent(candidate);
        }
    }

    @Test void compatibleInterferenceReevaluatesLifecycleOnceAndPreservesPaymentHistory(){
        saved();
        var bridge=new InterleavedRepository(1,current->
            repo.saveCurrent(current.recordPayment(AT.plusSeconds(1))));
        var service=new OrderWorkflowService(bridge,CLOCK);
        var outcome=service.recordRestaurantAcceptance(ID,RESTAURANT);
        assertThat(outcome).isInstanceOf(OrderActionResult.Accepted.class);
        assertThat(bridge.reads.get()).isEqualTo(2);
        assertThat(bridge.saves.get()).isEqualTo(2);
        assertThat(repo.findCurrentById(ID).orElseThrow().status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(version()).isEqualTo(2);
        assertThat(actions()).containsExactly(OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void incompatibleInterferenceReturnsDomainRejectionInsteadOfBlindlySaving(){
        saved();
        var bridge=new InterleavedRepository(1,current->
            repo.saveCurrent(current.recordRestaurantRejection(RESTAURANT,AT.plusSeconds(1))));
        var result=new OrderWorkflowService(bridge,CLOCK)
            .recordRestaurantAcceptance(ID,RESTAURANT);
        rejected(result,OrderActionRejection.Code.ILLEGAL_TRANSITION);
        assertThat(bridge.reads.get()).isEqualTo(2);
        assertThat(bridge.saves.get()).isEqualTo(1);
        assertThat(version()).isEqualTo(1);
        assertThat(actions()).containsExactly(OrderWorkflowAction.RESTAURANT_REJECTED);
    }

    @Test void twoConsecutiveStaleConflictsExhaustExactlyTwoAttempts(){
        saved();
        var bridge=new InterleavedRepository(2,current->
            repo.saveCurrent(current.recordPayment(AT.plusSeconds(1+current.version()))));
        var result=new OrderWorkflowService(bridge,CLOCK)
            .recordRestaurantAcceptance(ID,RESTAURANT);
        rejected(result,OrderActionRejection.Code.STALE_ORDER_VERSION);
        assertThat(bridge.reads.get()).isEqualTo(2);
        assertThat(bridge.saves.get()).isEqualTo(2);
        assertThat(version()).isEqualTo(2);
        assertThat(actions()).containsExactly(OrderWorkflowAction.PAYMENT_RECORDED,
                OrderWorkflowAction.PAYMENT_RECORDED);
        // Two payment occurrences are test-induced concurrent local facts:
        // a version conflict never implies provider-side idempotency.
    }

    @Test void localPaymentMilestoneDoesNotAutomaticallyRetryAfterStaleConflict(){
        saved();
        var bridge=new InterleavedRepository(1,current->
            repo.saveCurrent(current.recordPayment(AT.plusSeconds(1))));
        var result=new OrderWorkflowService(bridge,CLOCK).recordPayment(ID);
        rejected(result,OrderActionRejection.Code.STALE_ORDER_VERSION);
        assertThat(bridge.reads.get()).isEqualTo(1);
        assertThat(bridge.saves.get()).isEqualTo(1);
        assertThat(version()).isEqualTo(1);
        assertThat(actions()).containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
    }

    @Test void modificationDoesNotSilentlyReplayAfterConcurrentChange(){
        saved();
        var bridge=new InterleavedRepository(1,current->
            repo.saveCurrent(current.recordPayment(AT.plusSeconds(1))));
        var modify=new ModifyOrderService(bridge,CLOCK);
        var lines=List.of(new ModifyOrderCommand.Line(
            "water","Water",1,new BigDecimal("1.25")));
        var command=new ModifyOrderCommand(ID,CUSTOMER,lines);
        rejected(modify.modify(command),OrderActionRejection.Code.STALE_ORDER_VERSION);
        assertThat(bridge.reads.get()).isEqualTo(1);
        assertThat(bridge.saves.get()).isEqualTo(1);
        assertThat(repo.findCurrentById(ID).orElseThrow().lines()).isEqualTo(placed().lines());
        assertThat(actions()).containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
    }

    @Test void knownPostCommitLostAckDoesNotCauseAutomaticBusinessReplay(){
        var original=saved();
        var real=repo;
        var commits=new AtomicInteger();
        OrderRepository ambiguous=new OrderRepository(){
            @Override public void createNew(Order o){real.createNew(o);}
            @Override public Optional<Order> findCurrentById(OrderId id){
                return real.findCurrentById(id);
            }
            @Override public void saveCurrent(Order candidate){
                commits.incrementAndGet();
                real.saveCurrent(candidate); // real PostgreSQL COMMIT succeeds
                throw new OrderCommitOutcomeUnknownException(
                    "P18 controlled lost response AFTER real COMMIT",
                    candidate.id(),new SQLException("acknowledgement lost","08006"));
            }
        };
        var service=new OrderWorkflowService(ambiguous,CLOCK);
        assertThatThrownBy(()->service.recordRestaurantAcceptance(ID,RESTAURANT))
            .isInstanceOf(OrderCommitOutcomeUnknownException.class);
        assertThat(commits.get()).isEqualTo(1);
        assertThat(version()).isEqualTo(1);
        assertThat(actions()).containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        // A second re-execution has NOT occurred; P15's JDBC-proxy experiment
        // separately models loss at the actual Connection.commit boundary.
    }

    @Test void actualPostgresRowLockTimeoutIsClassifiedAndNotRetried() throws Exception{
        var original=saved();
        try(var locker=connection()){
            locker.setAutoCommit(false);
            try(var ps=locker.prepareStatement("UPDATE orders SET status=status WHERE id=?")){
                ps.setObject(1,ID.value());assertThat(ps.executeUpdate()).isEqualTo(1);
            }
            var faulted=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),
                pg.getPassword(),()->{
                    var c=connection();
                    try(var statement=c.createStatement()){
                        statement.execute("SET lock_timeout = '200ms'");
                    }
                    return c;
                });
            var service=new OrderWorkflowService(faulted,CLOCK);
            assertThatThrownBy(()->service.recordRestaurantAcceptance(ID,RESTAURANT))
                .isInstanceOf(OrderDatabaseConcurrencyException.class)
                .satisfies(ex->{
                    var typed=(OrderDatabaseConcurrencyException)ex;
                    assertThat(typed.kind())
                        .isEqualTo(OrderDatabaseConcurrencyException.Kind.LOCK_NOT_AVAILABLE);
                    assertThat(typed.sqlState()).isEqualTo("55P03");
                });
            locker.rollback();
        }
        assertThat(version()).isZero();
        assertThat(repo.findCurrentById(ID).orElseThrow().status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test void actualPostgresRepeatableReadConflictIsClassified40001() throws Exception {
        var initial=saved();
        var firstConnection=new AtomicInteger();
        var faulted=new PostgresOrderRepository(
            pg.getJdbcUrl(),pg.getUsername(),pg.getPassword(),()->{
                var db=connection();
                if(firstConnection.getAndIncrement()>0)return db;
                return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},(proxy,method,args)->{
                        if("setAutoCommit".equals(method.getName())
                            && Boolean.FALSE.equals(args[0])){
                            db.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                            db.setAutoCommit(false);
                            try(var s=db.createStatement();var rs=s.executeQuery(
                                    "SELECT version FROM orders WHERE id='"+ID+"'")){
                                assertThat(rs.next()).isTrue();
                                assertThat(rs.getLong(1)).isZero();
                            }
                            // Independent transaction changes version after
                            // the faulted transaction's snapshot was fixed.
                            repo.saveCurrent(initial.recordRestaurantAcceptance(
                                    RESTAURANT,AT.plusSeconds(1)));
                            return null;
                        }
                        try{return method.invoke(db,args);}
                        catch(InvocationTargetException e){throw e.getCause();}
                    });
            });
        var rejectedSuccessor=initial.recordRestaurantRejection(RESTAURANT,AT.plusSeconds(2));
        assertThatThrownBy(()->faulted.saveCurrent(rejectedSuccessor))
            .isInstanceOf(OrderDatabaseConcurrencyException.class)
            .satisfies(ex->{
                var typed=(OrderDatabaseConcurrencyException)ex;
                assertThat(typed.kind())
                    .isEqualTo(OrderDatabaseConcurrencyException.Kind.SERIALIZATION_FAILURE);
                assertThat(typed.sqlState()).isEqualTo("40001");
            });
        assertThat(repo.findCurrentById(ID).orElseThrow().status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(version()).isEqualTo(1);
    }
}
