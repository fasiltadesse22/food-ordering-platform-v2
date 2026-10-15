package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.*;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PostgresOrderRepositoryIntegrationTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repo;
    private static final Instant AT=Instant.parse("2026-10-12T10:30:20.123456Z");
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174221");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p12");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p12");

    @BeforeAll static void start() {
        pg=new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("ordering_p12_test")
                .withUsername("ordering")
                .withPassword("only-for-test");
        pg.start();
        repo=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
    }
    @AfterAll static void stop(){if(pg!=null)pg.stop();}
    @BeforeEach void clean() throws Exception {
        try(var db=DriverManager.getConnection(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var s=db.createStatement()) {
            s.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
        }
    }
    private static Order initial(){
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
                new OrderLine("burger-1","Burger",2,new BigDecimal("5.50")),
                new OrderLine("fries-1","Fries",1,new BigDecimal("3.75"))
        ),AT);
    }

    @Test void createsAndReconstitutesEntirePlacedAggregateAcrossRepositoryInstances(){
        var original=initial();
        repo.createNew(original);
        var second=new PostgresOrderRepository(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
        var recovered=second.findCurrentById(ID).orElseThrow();
        assertThat(recovered).isNotSameAs(original);
        assertThat(recovered.id()).isEqualTo(original.id());
        assertThat(recovered.customerId()).isEqualTo(CUSTOMER);
        assertThat(recovered.restaurantId()).isEqualTo(RESTAURANT);
        assertThat(recovered.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(recovered.placedAt()).isEqualTo(AT);
        assertThat(recovered.lines()).isEqualTo(original.lines());
        assertThat(recovered.workflowOccurrences()).isEmpty();
        assertThat(recovered.total()).isEqualByComparingTo("14.75");
    }

    @Test void lifecycleHistoryAndModifiedLinesReconstituteInPersistedOrder(){
        var original=initial();
        repo.createNew(original);
        var modified=original.modifyLines(CUSTOMER,List.of(
                new OrderLine("water-1","Water",1,new BigDecimal("1.25")),
                new OrderLine("burger-1","Burger",3,new BigDecimal("5.50"))
        ),AT.plusSeconds(1));
        repo.saveCurrent(modified);
        var paid=modified.recordPayment(AT.plusSeconds(2));
        repo.saveCurrent(paid);
        var cancelled=paid.recordCancellation(CUSTOMER,AT.plusSeconds(3));
        repo.saveCurrent(cancelled);
        var refund=cancelled.recordRefundRequest(AT.plusSeconds(4));
        repo.saveCurrent(refund);

        var recovered=new PostgresOrderRepository(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword()).findCurrentById(ID).orElseThrow();
        assertThat(recovered.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(recovered.lines()).isEqualTo(modified.lines());
        assertThat(recovered.total()).isEqualByComparingTo("17.75");
        assertThat(recovered.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(OrderWorkflowAction.ORDER_MODIFIED,
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED);
        assertThat(recovered.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::occurredAt)
                .containsExactly(AT.plusSeconds(1),AT.plusSeconds(2),
                        AT.plusSeconds(3),AT.plusSeconds(4));
    }

    @Test void createCollisionDoesNotOverwriteCurrentDatabaseOrder(){
        var initial=initial();
        repo.createNew(initial);
        assertThatThrownBy(()->repo.createNew(initial))
                .isInstanceOf(OrderAlreadyExistsException.class);
        var found=repo.findCurrentById(ID).orElseThrow();
        assertThat(found.lines()).isEqualTo(initial.lines());
        assertThat(found.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test void missingAggregateIsEmptyNotAnInvalidRepresentation(){
        assertThat(repo.findCurrentById(ID)).isEmpty();
    }

    @Test void completedStatusWithoutRequiredHistoryIsCorruptionNotAbsence() throws Exception {
        repo.createNew(initial());
        try(var db=DriverManager.getConnection(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=db.prepareStatement("UPDATE orders SET status='COMPLETED' WHERE id=?")) {
            ps.setObject(1,ID.value());ps.executeUpdate();
        }
        assertThatThrownBy(()->repo.findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
    }

    @Test void databaseNowRejectsInvalidLineQuantityWithoutCorruptingExistingAggregate() throws Exception {
        repo.createNew(initial());
        try(var db=DriverManager.getConnection(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=db.prepareStatement(
                "UPDATE order_lines SET quantity=0 WHERE order_id=? AND line_position=0")) {
            ps.setObject(1,ID.value());
            assertThatThrownBy(ps::executeUpdate)
                    .isInstanceOf(java.sql.SQLException.class)
                    .satisfies(e->assertThat(((java.sql.SQLException)e).getSQLState()).isEqualTo("23514"));
        }
        var stillValid=repo.findCurrentById(ID).orElseThrow();
        assertThat(stillValid.lines().get(0).quantity()).isEqualTo(2);
        assertThat(stillValid.total()).isEqualByComparingTo("14.75");
        // Invalid status/history and missing-lines corruption remain separate tests.
    }

    @Test void staleCancellationNowFailsVersionCheckWithoutErasingCommittedAcceptance(){
        var original=initial();
        repo.createNew(original);
        var accepted=original.recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        var staleCancel=original.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repo.saveCurrent(accepted);
        assertThatThrownBy(()->repo.saveCurrent(staleCancel))
                .isInstanceOf(StaleOrderVersionException.class);
        var recovered=repo.findCurrentById(ID).orElseThrow();
        assertThat(recovered.version()).isEqualTo(1);
        assertThat(recovered.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(recovered.workflowOccurrences())
                .extracting(OrderWorkflowOccurrence::action)
                .containsExactly(OrderWorkflowAction.RESTAURANT_ACCEPTED);
    }

    @Test void versionedSaveCannotInventAbsentIdentity(){
        var successor=initial().recordPayment(AT.plusSeconds(1));
        assertThatThrownBy(()->repo.saveCurrent(successor))
                .isInstanceOf(StaleOrderVersionException.class);
        assertThat(repo.findCurrentById(ID)).isEmpty();
    }

    @Test void storedOrderWithMissingLinesIsRejectedDuringReconstitution() throws Exception {
        repo.createNew(initial());
        try(var db=DriverManager.getConnection(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=db.prepareStatement("DELETE FROM order_lines WHERE order_id=?")) {
            ps.setObject(1,ID.value());ps.executeUpdate();
        }
        assertThatThrownBy(()->repo.findCurrentById(ID))
                .isInstanceOf(InvalidPersistedOrderException.class);
    }
}
