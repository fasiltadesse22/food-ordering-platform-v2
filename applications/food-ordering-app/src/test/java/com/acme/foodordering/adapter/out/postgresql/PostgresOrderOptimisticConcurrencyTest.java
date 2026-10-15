package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.*;
import com.acme.foodordering.application.service.OrderWorkflowService;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/** P17 real PostgreSQL conditional commit tests (not a mocked database).
 * Each test starts from a freshly truncated aggregate; the original P16
 * both-accepted race is preserved in the earlier frozen checkpoint.
 */
class PostgresOrderOptimisticConcurrencyTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174271");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p17");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p17");
    private static final Instant AT=Instant.parse("2026-10-15T18:00:00.123456Z");

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p17")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}

    @BeforeEach void reset() throws SQLException {
        try(var c=connection();var s=c.createStatement()){
            s.execute("DROP FUNCTION IF EXISTS p17_injected_history_failure() CASCADE");
            s.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
        }
    }
    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    private static Order placed(){
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",2,new BigDecimal("5.50")),
            new OrderLine("water","Water",1,new BigDecimal("1.25"))),AT);
    }
    private static void gate(CountDownLatch latch,String label) {
        try {if(!latch.await(12,TimeUnit.SECONDS))
            throw new IllegalStateException("timeout waiting for "+label);
        } catch(InterruptedException ex){
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
    private static long dbVersion() throws SQLException {
        try(var c=connection();var ps=c.prepareStatement(
                "SELECT version FROM orders WHERE id=?")){
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                return rs.getLong(1);
            }
        }
    }

    @Test void placementHasRevisionZeroAndSuccessorsAdvanceAcrossReconstitution()
            throws Exception {
        var initial=placed();
        assertThat(initial.version()).isZero();
        repo.createNew(initial);
        assertThat(dbVersion()).isZero();
        var first=repo.findCurrentById(ID).orElseThrow();
        assertThat(first.version()).isZero();
        var accepted=first.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        assertThat(accepted.version()).isEqualTo(1);
        repo.saveCurrent(accepted);
        assertThat(dbVersion()).isEqualTo(1);
        var second=repo.findCurrentById(ID).orElseThrow();
        assertThat(second.version()).isEqualTo(1);
        var preparing=second.recordPreparationStarted(RESTAURANT,AT.plusSeconds(2));
        assertThat(preparing.version()).isEqualTo(2);
        repo.saveCurrent(preparing);
        var third=repo.findCurrentById(ID).orElseThrow();
        assertThat(third.version()).isEqualTo(2);
        assertThat(third.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(third.workflowOccurrences())
            .extracting(w->w.action())
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED,
                    OrderWorkflowAction.PREPARATION_STARTED);
    }

    @Test void staleCancellationDoesNotEraseCommittedAcceptance() throws Exception {
        var initial=placed();repo.createNew(initial);
        var cancellation=initial.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repo.saveCurrent(initial.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1)));
        assertThatThrownBy(()->repo.saveCurrent(cancellation))
            .isInstanceOf(StaleOrderVersionException.class)
            .satisfies(ex->assertThat(((StaleOrderVersionException)ex).expectedVersion()).isZero());
        var winner=repo.findCurrentById(ID).orElseThrow();
        assertThat(winner.version()).isEqualTo(1);
        assertThat(winner.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(winner.workflowOccurrences()).extracting(w->w.action())
            .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        assertThat(winner.lines()).isEqualTo(initial.lines());
    }

    @Test void duplicatePaymentMilestoneFromSameSnapshotIsRejected() {
        var initial=placed();repo.createNew(initial);
        var first=initial.recordPayment(AT.plusSeconds(1));
        var competing=initial.recordPayment(AT.plusSeconds(2));
        repo.saveCurrent(first);
        assertThatThrownBy(()->repo.saveCurrent(competing))
            .isInstanceOf(StaleOrderVersionException.class);
        var current=repo.findCurrentById(ID).orElseThrow();
        assertThat(current.version()).isEqualTo(1);
        assertThat(current.workflowOccurrences()).extracting(w->w.action())
            .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
        // Prevents this stale replacement, NOT general payment command
        // idempotency or provider-side duplicate charging.
    }

    @Test void retryingExactlySameSuccessorCannotAdvanceTwice() {
        var initial=placed();repo.createNew(initial);
        var successor=initial.recordPayment(AT.plusSeconds(1));
        repo.saveCurrent(successor);
        assertThatThrownBy(()->repo.saveCurrent(successor))
            .isInstanceOf(StaleOrderVersionException.class);
        assertThat(repo.findCurrentById(ID).orElseThrow().version()).isEqualTo(1);
    }

    @Test void attemptedSaveOfAbsentIdentityCannotCreateOrder() {
        var candidate=placed().recordPayment(AT.plusSeconds(1));
        assertThatThrownBy(()->repo.saveCurrent(candidate))
            .isInstanceOf(StaleOrderVersionException.class);
        assertThat(repo.findCurrentById(ID)).isEmpty();
    }

    @Test void forgedFutureRevisionCannotSkipStoredRevision() {
        var initial=placed();repo.createNew(initial);
        var bogus=Order.reconstitute(ID,CUSTOMER,RESTAURANT,initial.lines(),
                OrderStatus.PLACED,AT,List.of(),5L).recordPayment(AT.plusSeconds(1));
        assertThat(bogus.version()).isEqualTo(6);
        assertThatThrownBy(()->repo.saveCurrent(bogus))
            .isInstanceOf(StaleOrderVersionException.class)
            .satisfies(e->assertThat(((StaleOrderVersionException)e).expectedVersion()).isEqualTo(5));
        assertThat(repo.findCurrentById(ID).orElseThrow().version()).isZero();
    }

    @Test void concurrentAcceptanceAndRejectionYieldExactlyOneCommittedWinner()
            throws Exception {
        repo.createNew(placed());
        var bothRead=new CountDownLatch(2);
        var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var acceptance=pool.submit(()->{
                var base=repo.findCurrentById(ID).orElseThrow();
                bothRead.countDown();gate(go,"start");
                try {
                    repo.saveCurrent(base.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1)));
                    return true;
                } catch(StaleOrderVersionException ex){return false;}
            });
            var rejection=pool.submit(()->{
                var base=repo.findCurrentById(ID).orElseThrow();
                bothRead.countDown();gate(go,"start");
                try {
                    repo.saveCurrent(base.recordRestaurantRejection(RESTAURANT,AT.plusSeconds(2)));
                    return true;
                } catch(StaleOrderVersionException ex){return false;}
            });
            gate(bothRead,"both current snapshots");
            go.countDown();
            assertThat(acceptance.get(12,TimeUnit.SECONDS)
                    !=rejection.get(12,TimeUnit.SECONDS)).isTrue();
        } finally{go.countDown();}
        var result=repo.findCurrentById(ID).orElseThrow();
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.status()).isIn(OrderStatus.ACCEPTED,OrderStatus.REJECTED);
        assertThat(result.workflowOccurrences()).hasSize(1);
        assertThat(result.workflowOccurrences().get(0).action()).isIn(
            OrderWorkflowAction.RESTAURANT_ACCEPTED,OrderWorkflowAction.RESTAURANT_REJECTED);
    }

    @Test void failedHistoryWriteAfterConditionalVersionUpdateRollsBackRevision()
            throws Exception {
        var initial=placed();repo.createNew(initial);
        try(var c=connection();var s=c.createStatement()){
            s.execute("CREATE FUNCTION p17_injected_history_failure() RETURNS trigger "
                +"LANGUAGE plpgsql AS $$ BEGIN "
                +"RAISE EXCEPTION 'P17 injected history failure' USING ERRCODE='P0001'; "
                +"END; $$");
            s.execute("CREATE TRIGGER p17_fail_history BEFORE INSERT "
                +"ON order_workflow_occurrences FOR EACH ROW "
                +"EXECUTE FUNCTION p17_injected_history_failure()");
        }
        var updated=initial.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        assertThatThrownBy(()->repo.saveCurrent(updated))
            .isInstanceOf(OrderPersistenceException.class)
            .isNotInstanceOf(OrderCommitOutcomeUnknownException.class);
        var recovered=repo.findCurrentById(ID).orElseThrow();
        assertThat(recovered.version()).isZero();
        assertThat(recovered.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(recovered.lines()).isEqualTo(initial.lines());
        assertThat(recovered.workflowOccurrences()).isEmpty();
        assertThat(dbVersion()).isZero();
    }

    @Test void applicationServiceReevaluatesStaleLifecycleDecisionAndRejectsNowIllegalTransition()
            throws Exception {
        repo.createNew(placed());
        var reads=new CountDownLatch(2);
        var go=new CountDownLatch(1);
        var guarded=new OrderRepository(){
            @Override public void createNew(Order order){repo.createNew(order);}
            @Override public void saveCurrent(Order order){repo.saveCurrent(order);}
            @Override public Optional<Order> findCurrentById(OrderId id){
                var loaded=repo.findCurrentById(id);
                reads.countDown();
                gate(go,"service start");
                return loaded;
            }
        };
        var service=new OrderWorkflowService(guarded,
                Clock.fixed(AT.plusSeconds(3),ZoneOffset.UTC));
        try(var pool=Executors.newFixedThreadPool(2)){
            var accept=pool.submit(()->service.recordRestaurantAcceptance(ID,RESTAURANT));
            var reject=pool.submit(()->service.recordRestaurantRejection(ID,RESTAURANT));
            gate(reads,"service reads");
            go.countDown();
            var a=accept.get(12,TimeUnit.SECONDS);
            var b=reject.get(12,TimeUnit.SECONDS);
            long successful=List.of(a,b).stream()
                .filter(x->x instanceof OrderActionResult.Accepted).count();
            long nowIllegal=List.of(a,b).stream()
                .filter(x->x instanceof OrderActionResult.Rejected rejected
                    && rejected.rejection().code()
                        ==com.acme.foodordering.application.port.in.OrderActionRejection.Code.ILLEGAL_TRANSITION)
                .count();
            assertThat(successful).isEqualTo(1);
            assertThat(nowIllegal).isEqualTo(1);
            // P17's frozen checkpoint proves the raw stale-version rejection.
            // P18 now deliberately rereads and returns the NEW domain outcome.
        } finally{go.countDown();}
    }

    @Test void versionConstraintRejectsNegativeRawDatabaseValue() throws Exception {
        repo.createNew(placed());
        try(var c=connection();var ps=c.prepareStatement(
                "UPDATE orders SET version=-1 WHERE id=?")){
            ps.setObject(1,ID.value());
            assertThatThrownBy(ps::executeUpdate)
                .isInstanceOf(SQLException.class)
                .satisfies(e->assertThat(((SQLException)e).getSQLState()).isEqualTo("23514"));
        }
        assertThat(dbVersion()).isZero();
    }
}
