package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.StaleOrderVersionException;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * C1.2-P19: controlled, real-PostgreSQL aggregate-size and contention
 * characterization. Diagnostic nanosecond samples are environment dependent:
 * never assert monotonic latency or claim production throughput/SLOs.
 * Fixture history is synthetic ORDER_MODIFIED local facts, not financial
 * payment-provider events or a real production distribution.
 */
class PostgresOrderAggregateSizingTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final CustomerId CUSTOMER=new CustomerId("customer-p19");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p19");
    private static final Instant AT=Instant.parse("2026-10-15T18:00:00.123456Z");
    private static final AtomicLong IDS=new AtomicLong(300);
    private static final OrderId HOT_ID=OrderId.from("123e4567-e89b-12d3-a456-426614174300");

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("ordering_p19")
                .withUsername("ordering")
                .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    @BeforeEach void clean() throws SQLException {
        try(var db=connection();var stmt=db.createStatement()){
            stmt.execute("DROP TABLE IF EXISTS p19_churn_audit CASCADE");
            stmt.execute("DROP FUNCTION IF EXISTS p19_audit_churn() CASCADE");
            stmt.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
        }
    }
    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    private static OrderId uniqueId() {
        return OrderId.from("123e4567-e89b-12d3-a456-42661417"+
                String.format("%04d",IDS.incrementAndGet()));
    }
    private static void await(CountDownLatch latch){
        try {
            if(!latch.await(15,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();throw new IllegalStateException(e);
        }
    }
    private static List<OrderLine> lines(int count) {
        var result=new ArrayList<OrderLine>();
        for(int i=0;i<count;i++)
            result.add(new OrderLine("item-"+i,"Item "+i,1+(i%3),
                    new BigDecimal("1.25")));
        return List.copyOf(result);
    }
    private static List<OrderWorkflowOccurrence> history(int count) {
        var result=new ArrayList<OrderWorkflowOccurrence>();
        for(int i=0;i<count;i++)
            result.add(new OrderWorkflowOccurrence(
                    OrderWorkflowAction.ORDER_MODIFIED,WorkflowParticipant.CUSTOMER,
                    AT.plusSeconds(i+1)));
        return List.copyOf(result);
    }
    private static Order seed(OrderId id,int lineCount,int historyCount) {
        // Test fixture models an existing PLACED Order with a V3 migration
        // baseline version=0. History is synthetic, locally valid, and
        // explicitly NOT proof of actual historical command count.
        var order=Order.reconstitute(id,CUSTOMER,RESTAURANT,
                lines(lineCount),OrderStatus.PLACED,AT,history(historyCount),0);
        repo.createNew(order);
        return order;
    }
    private static long count(String table,OrderId id) throws SQLException{
        try(var db=connection();
            var ps=db.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE order_id=?")){
            ps.setObject(1,id.value());
            try(var rs=ps.executeQuery()){rs.next();return rs.getLong(1);}
        }
    }
    private static long elapsed(Runnable action){
        long start=System.nanoTime();action.run();return System.nanoTime()-start;
    }

    @Test void workloadMatrixMeasuresObservedReadAndWriteSamplesWithoutLatencySla()
            throws Exception {
        int[][] profiles={{1,0},{8,16},{48,96}};
        for(int[] profile:profiles){
            int L=profile[0],H=profile[1];
            var source=seed(uniqueId(),L,H);
            var readSamples=new long[3];
            for(int i=0;i<3;i++) {
                int index=i;
                readSamples[index]=elapsed(()->{
                    var found=repo.findCurrentById(source.id()).orElseThrow();
                    assertThat(found.lines()).hasSize(L);
                    assertThat(found.workflowOccurrences()).hasSize(H);
                    assertThat(found.version()).isZero();
                });
            }
            var successor=source.recordPayment(AT.plusSeconds(H+10));
            long writeNs=elapsed(()->repo.saveCurrent(successor));
            var stored=repo.findCurrentById(source.id()).orElseThrow();
            assertThat(stored.lines()).hasSize(L);
            assertThat(stored.workflowOccurrences()).hasSize(H+1);
            assertThat(stored.version()).isEqualTo(1);
            assertThat(count("order_lines",source.id())).isEqualTo(L);
            assertThat(count("order_workflow_occurrences",source.id())).isEqualTo(H+1);
            java.util.Arrays.sort(readSamples);
            System.out.printf(
                "P19_SAMPLE lines=%d historyBefore=%d rowsRead=%d "
                +"rowsReplacedOrInserted=%d readMedianNs=%d writeNs=%d "
                +"limitation=CI_specific_no_performance_SLO%n",
                L,H,1+L+H,2*L+2*H+1,readSamples[1],writeNs);
        }
    }

    @Test void databaseTriggersCountEntireChildCollectionRewriteForOneMilestone()
            throws Exception {
        int L=12,H=24;
        var source=seed(uniqueId(),L,H);
        try(var db=connection();var st=db.createStatement()){
            st.execute("CREATE TABLE p19_churn_audit(collection_name TEXT NOT NULL, action TEXT NOT NULL)");
            st.execute("CREATE FUNCTION p19_audit_churn() RETURNS trigger LANGUAGE plpgsql AS $$ "
                +"BEGIN INSERT INTO p19_churn_audit VALUES(TG_TABLE_NAME,TG_OP); "
                +"RETURN NULL; END; $$");
            st.execute("CREATE TRIGGER p19_lines AFTER DELETE OR INSERT ON order_lines "
                +"FOR EACH ROW EXECUTE FUNCTION p19_audit_churn()");
            st.execute("CREATE TRIGGER p19_history AFTER DELETE OR INSERT ON order_workflow_occurrences "
                +"FOR EACH ROW EXECUTE FUNCTION p19_audit_churn()");
        }
        repo.saveCurrent(source.recordPayment(AT.plusSeconds(30)));
        assertThat(auditCount("order_lines","DELETE")).isEqualTo(L);
        assertThat(auditCount("order_lines","INSERT")).isEqualTo(L);
        assertThat(auditCount("order_workflow_occurrences","DELETE")).isEqualTo(H);
        assertThat(auditCount("order_workflow_occurrences","INSERT")).isEqualTo(H+1);
        System.out.printf(
            "P19_CHURN_AUDIT linesDeleted=%d linesInserted=%d "
                +"historyDeleted=%d historyInserted=%d totalChildRowOperations=%d%n",
            L,L,H,H+1,2*L+2*H+1);
        var found=repo.findCurrentById(source.id()).orElseThrow();
        assertThat(found.version()).isEqualTo(1);
        assertThat(found.workflowOccurrences()).hasSize(H+1);
    }
    private static long auditCount(String collection,String action) throws SQLException{
        try(var db=connection();var ps=db.prepareStatement(
                "SELECT COUNT(*) FROM p19_churn_audit WHERE collection_name=? AND action=?")){
            ps.setString(1,collection);ps.setString(2,action);
            try(var rs=ps.executeQuery()){rs.next();return rs.getLong(1);}
        }
    }

    @Test void unrelatedWithinOrderChangesShareOneCasHotspotEvenWhenRowsDiffer()
            throws Exception {
        seed(HOT_ID,48,96);
        var ready=new CountDownLatch(2);
        var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var edit=pool.submit(()->{
                var base=repo.findCurrentById(HOT_ID).orElseThrow();
                ready.countDown();await(go);
                try{
                    repo.saveCurrent(base.modifyLines(CUSTOMER,lines(49),AT.plusSeconds(100)));
                    return true;
                }catch(StaleOrderVersionException e){return false;}
            });
            var milestone=pool.submit(()->{
                var base=repo.findCurrentById(HOT_ID).orElseThrow();
                ready.countDown();await(go);
                try{
                    repo.saveCurrent(base.recordPayment(AT.plusSeconds(101)));
                    return true;
                }catch(StaleOrderVersionException e){return false;}
            });
            await(ready);go.countDown();
            boolean a=edit.get(15,TimeUnit.SECONDS);
            boolean b=milestone.get(15,TimeUnit.SECONDS);
            assertThat(a!=b).isTrue();
            var committed=repo.findCurrentById(HOT_ID).orElseThrow();
            assertThat(committed.version()).isEqualTo(1);
            assertThat(committed.workflowOccurrences()).hasSize(97);
            assertThat(committed.lines().size()).isIn(48,49);
            System.out.printf(
                "P19_HOT_ORDER logicalOperations=lineChange_vs_paymentMilestone "
                +"commonPredecessor=0 successes=%d conflicts=%d "
                +"finalVersion=%d%n",(a?1:0)+(b?1:0),(a?0:1)+(b?0:1),committed.version());
        } finally {go.countDown();}
    }

    @Test void distinctOrdersAdvanceIndependentlyWithoutAnAggregateWideGlobalLock()
            throws Exception {
        var left=seed(uniqueId(),48,96);
        var right=seed(uniqueId(),48,96);
        var ready=new CountDownLatch(2);
        var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{
                var base=repo.findCurrentById(left.id()).orElseThrow();
                ready.countDown();await(go);
                repo.saveCurrent(base.recordPayment(AT.plusSeconds(120)));
                return true;
            });
            var b=pool.submit(()->{
                var base=repo.findCurrentById(right.id()).orElseThrow();
                ready.countDown();await(go);
                repo.saveCurrent(base.recordPayment(AT.plusSeconds(121)));
                return true;
            });
            await(ready);go.countDown();
            assertThat(a.get(15,TimeUnit.SECONDS)).isTrue();
            assertThat(b.get(15,TimeUnit.SECONDS)).isTrue();
        } finally {go.countDown();}
        assertThat(repo.findCurrentById(left.id()).orElseThrow().version()).isEqualTo(1);
        assertThat(repo.findCurrentById(right.id()).orElseThrow().version()).isEqualTo(1);
        System.out.println("P19_DISTINCT_ORDERS independentVersionAdvances=2 conflicts=0");
    }

    @Test void largeAggregateVersionConflictPreservesAllPriorOwnedRows()
            throws Exception{
        var source=seed(uniqueId(),48,96);
        var winner=source.recordPayment(AT.plusSeconds(160));
        var stale=source.modifyLines(CUSTOMER,lines(65),AT.plusSeconds(161));
        repo.saveCurrent(winner);
        assertThatThrownBy(()->repo.saveCurrent(stale))
            .isInstanceOf(StaleOrderVersionException.class);
        var found=repo.findCurrentById(source.id()).orElseThrow();
        assertThat(found.version()).isEqualTo(1);
        assertThat(found.lines()).isEqualTo(source.lines());
        assertThat(found.workflowOccurrences()).hasSize(97);
        assertThat(count("order_lines",source.id())).isEqualTo(48);
        assertThat(count("order_workflow_occurrences",source.id())).isEqualTo(97);
    }
}
