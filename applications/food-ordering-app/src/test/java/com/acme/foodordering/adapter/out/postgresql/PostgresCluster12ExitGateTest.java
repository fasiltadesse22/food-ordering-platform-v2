package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.InvalidPersistedOrderException;
import com.acme.foodordering.application.port.out.StaleOrderVersionException;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.inventory.InventoryAvailabilitySnapshot;
import com.acme.foodordering.domain.inventory.OrderInventoryReservationPolicy;
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
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * C1.2-P22 cluster-level contract tests: real PostgreSQL V1-V3, real root,
 * real application services, currentness, reconstitution, and domain authority.
 * Preserve preceding dedicated P11-P21 experiments (these are NOT substitutes).
 * Stock observation is advisory; no production Inventory or Payment writer.
 */
class PostgresCluster12ExitGateTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final OrderId ID =
            OrderId.from("123e4567-e89b-12d3-a456-426614174325");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p22");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p22");
    private static final Instant AT=Instant.parse("2026-10-16T14:15:20.123456Z");
    private static final Clock CLOCK=Clock.fixed(AT.plusSeconds(100), ZoneOffset.UTC);

    @BeforeAll static void start(){
        pg=new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("ordering_p22")
                .withUsername("ordering")
                .withPassword("test-only-password");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    @BeforeEach void clean() throws Exception {
        try(var db=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var st=db.createStatement()){
            st.execute("TRUNCATE order_workflow_occurrences,order_lines,orders");
        }
    }
    private static Order placed(){
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
                new OrderLine("burger","Burger",2,new BigDecimal("5.50")),
                new OrderLine("water","Water",1,new BigDecimal("1.25"))),AT);
    }
    private static PostgresOrderRepository newReader(){
        return new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }

    @Test void completeActualAggregateLifecycleSurvivesFreshRepositoryReconstitution(){
        var first=placed();
        repo.createNew(first);
        var modified=first.modifyLines(CUSTOMER,List.of(
                new OrderLine("burger","Burger",3,new BigDecimal("5.50")),
                new OrderLine("water","Water",1,new BigDecimal("1.25"))),AT.plusSeconds(1));
        repo.saveCurrent(modified);
        var paid=modified.recordPayment(AT.plusSeconds(2));
        repo.saveCurrent(paid);
        var cancelled=paid.recordCancellation(CUSTOMER,AT.plusSeconds(3));
        repo.saveCurrent(cancelled);
        var refund=cancelled.recordRefundRequest(AT.plusSeconds(4));
        repo.saveCurrent(refund);

        var recovered=newReader().findCurrentById(ID).orElseThrow();
        assertThat(recovered).isNotSameAs(refund);
        assertThat(recovered.id()).isEqualTo(first.id());
        assertThat(recovered.version()).isEqualTo(4);
        assertThat(recovered.customerId()).isEqualTo(CUSTOMER);
        assertThat(recovered.restaurantId()).isEqualTo(RESTAURANT);
        assertThat(recovered.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(recovered.lines()).isEqualTo(modified.lines());
        assertThat(recovered.total()).isEqualByComparingTo("17.75");
        assertThat(recovered.workflowOccurrences()).extracting(w->w.action())
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED,
                    OrderWorkflowAction.PAYMENT_RECORDED,
                    OrderWorkflowAction.ORDER_CANCELLED,
                    OrderWorkflowAction.REFUND_REQUESTED);
        System.out.println("C12_EXIT_LIFECYCLE version=4 status=CANCELLED orderedFacts=4 independentRead=PASS");
    }

    @Test void samePredecessorCannotPublishTwoConflictingLocalDecisions(){
        var first=placed();
        repo.createNew(first);
        var accepted=first.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        var staleCancellation=first.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repo.saveCurrent(accepted);
        assertThatThrownBy(()->repo.saveCurrent(staleCancellation))
                .isInstanceOf(StaleOrderVersionException.class);
        var actual=newReader().findCurrentById(ID).orElseThrow();
        assertThat(actual.version()).isEqualTo(1);
        assertThat(actual.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(actual.workflowOccurrences()).extracting(w->w.action())
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
        System.out.println("C12_EXIT_STALE firstCommitted=ACCEPTED staleCancelRejected=YES historyPreserved=YES");
    }

    @Test void domainGuardPreventsWrongRestaurantFromChangingPersistedOrder(){
        repo.createNew(placed());
        var service=new OrderWorkflowService(repo,CLOCK);
        var result=service.recordRestaurantAcceptance(ID,new RestaurantId("restaurant-other"));
        assertThat(result).isInstanceOfSatisfying(OrderActionResult.Rejected.class,
                rejected->assertThat(rejected.rejection().code())
                        .isEqualTo(OrderActionRejection.Code.RESTAURANT_DOES_NOT_OWN_ORDER));
        var persisted=newReader().findCurrentById(ID).orElseThrow();
        assertThat(persisted.version()).isZero();
        assertThat(persisted.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(persisted.workflowOccurrences()).isEmpty();
        System.out.println("C12_EXIT_RESTAURANT_GUARD rejectedWrongActor=YES noSave=YES");
    }

    @Test void rowValidButCrossHistoryInvalidOrderFailsDomainReconstitution()
            throws Exception {
        repo.createNew(placed());
        try(var db=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=db.prepareStatement("UPDATE orders SET status='ACCEPTED' WHERE id=?")){
            ps.setObject(1,ID.value());
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
        assertThatThrownBy(()->newReader().findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
        System.out.println("C12_EXIT_INVARIANT validRootRow=YES missingAcceptance=YES reconstitutionRejected=YES");
    }

    @Test void inventoryPlanningIsNotAPersistedOrderOrStockAdmission(){
        var order=placed();
        var proposal=new OrderInventoryReservationPolicy().propose(order,
            new InventoryAvailabilitySnapshot(RESTAURANT,9,
                Map.of("burger",4,"water",2)));
        assertThat(proposal.orderId()).isEqualTo(ID);
        assertThat(proposal.observedInventoryVersion()).isEqualTo(9);
        assertThat(proposal.requestedByMenuItem())
                .containsEntry("burger",2).containsEntry("water",1);
        assertThat(repo.findCurrentById(ID)).isEmpty();
        // A planning result is not an Order INSERT, stock decrement, provider
        // charge, or production reservation. P21 admission tests are isolated.
        System.out.println("C12_EXIT_AUTHORITY inventoryProposal=YES orderPersisted=NO stockAdmitted=NOT_IMPLEMENTED");
    }
}
