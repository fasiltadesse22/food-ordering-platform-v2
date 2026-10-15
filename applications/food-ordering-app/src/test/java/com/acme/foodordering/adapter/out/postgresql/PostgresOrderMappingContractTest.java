package com.acme.foodordering.adapter.out.postgresql;

import com.acme.foodordering.application.port.out.InvalidPersistedOrderException;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * P14 representation experiments: no ORM or new SQL schema. Real PostgreSQL
 * round-trip, ordered collection shape, decimal/time representation and JOIN
 * row cardinality. Does not claim transaction isolation or version safety.
 */
class PostgresOrderMappingContractTest {
    private static PostgreSQLContainer pg;
    private static PostgresOrderRepository repository;
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174241");
    private static final CustomerId CUSTOMER=new CustomerId("customer-p14");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-p14");
    private static final Instant AT=Instant.parse("2026-10-12T10:30:20.123456Z");

    @BeforeAll static void start() {
        pg=new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p14")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        repository=new PostgresOrderRepository(
                pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    @AfterAll static void stop() {if(pg!=null)pg.stop();}

    @BeforeEach void reset() throws SQLException {
        try(var c=DriverManager.getConnection(
                    pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=c.createStatement()) {
            ps.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
        }
    }

    private static Order initial() {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",2,new BigDecimal("5.5000")),
            new OrderLine("fries","Fries",1,new BigDecimal("3.7500")),
            new OrderLine("water","Water",1,new BigDecimal("1.2500"))),AT);
    }

    private static void sql(String query) throws SQLException {
        try(var c=DriverManager.getConnection(
                pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var statement=c.createStatement()) {
            statement.execute(query);
        }
    }

    @Test void rejectsMissingFirstLinePositionInsteadOfSilentlyCompressingList() throws Exception {
        repository.createNew(initial());
        sql("UPDATE order_lines SET line_position=7 WHERE order_id='" + ID
             + "' AND line_position=0");
        assertThatThrownBy(()->repository.findCurrentById(ID))
            .isInstanceOf(InvalidPersistedOrderException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsGapInsideLineCollectionDespiteValidV2CheckConstraints() throws Exception {
        repository.createNew(initial());
        sql("UPDATE order_lines SET line_position=4 WHERE order_id='" + ID
             + "' AND line_position=2");
        assertThatThrownBy(()->repository.findCurrentById(ID))
            .isInstanceOf(InvalidPersistedOrderException.class);
        // P13 CHECK >=0 and composite PK allow this gap. Mapper does not.
    }

    @Test void rejectsGapInWorkflowOccurrencesWithoutFakingOrDroppingHistory() throws Exception {
        var placed=initial();
        repository.createNew(placed);
        var paid=placed.recordPayment(AT.plusSeconds(1));
        repository.saveCurrent(paid);
        var cancelled=paid.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repository.saveCurrent(cancelled);
        sql("UPDATE order_workflow_occurrences SET occurrence_position=5 WHERE order_id='"
             + ID + "' AND occurrence_position=1");
        assertThatThrownBy(()->repository.findCurrentById(ID))
            .isInstanceOf(InvalidPersistedOrderException.class);
    }

    @Test void reconstitutesMultipleOrderedCollectionsAsDomainValues() {
        var placed=initial();
        repository.createNew(placed);
        var paid=placed.recordPayment(AT.plusSeconds(1));
        repository.saveCurrent(paid);
        var cancelled=paid.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repository.saveCurrent(cancelled);
        var loaded=repository.findCurrentById(ID).orElseThrow();
        assertThat(loaded).isNotSameAs(cancelled);
        assertThat(loaded.id()).isEqualTo(ID);
        assertThat(loaded.lines()).extracting(OrderLine::menuItemId)
            .containsExactly("burger","fries","water");
        assertThat(loaded.workflowOccurrences()).extracting(w->w.action())
            .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED,
                    OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(loaded.total()).isEqualByComparingTo("16.00");
    }

    @Test void numericRoundTripPreservesMagnitudeAndMeasuresScaleSeparately() {
        repository.createNew(initial());
        var loaded=repository.findCurrentById(ID).orElseThrow();
        var before=initial().lines().get(0).unitPrice();
        var after=loaded.lines().get(0).unitPrice();
        assertThat(after).isEqualByComparingTo(before);
        assertThat(loaded.total()).isEqualByComparingTo(initial().total());
        // Java BigDecimal.equals is scale-sensitive whereas compareTo is not.
        assertThat(new BigDecimal("5.5").equals(new BigDecimal("5.5000"))).isFalse();
        System.out.println("P14 observed NUMERIC scale: before="+before.scale()
                +" after="+after.scale()+", numerically equal="+(after.compareTo(before)==0));
    }

    @Test void timestampRoundTripExposesNanosecondToMicrosecondPrecisionBoundary() {
        var nanos=Instant.parse("2026-10-12T10:30:20.123456789Z");
        var src=Order.place(ID,CUSTOMER,RESTAURANT,List.of(
            new OrderLine("burger","Burger",1,new BigDecimal("1.00"))),nanos);
        repository.createNew(src);
        var loaded=repository.findCurrentById(ID).orElseThrow();
        var delta=Duration.between(nanos,loaded.placedAt()).abs();
        assertThat(loaded.placedAt()).isNotEqualTo(nanos);
        assertThat(delta).isLessThan(Duration.ofNanos(1000));
        System.out.println("P14 observed Instant: java="+nanos+
                " postgresRecovered="+loaded.placedAt()+" delta="+delta.toNanos()+"ns");
        // Do not silently change domain timestamps or invent rounding policy.
    }

    @Test void joiningTwoOwnedCollectionsMultipliesRowsInsteadOfYieldingOneOrder() throws Exception {
        var placed=initial();
        repository.createNew(placed);
        var step=placed.recordPayment(AT.plusSeconds(1));
        repository.saveCurrent(step);
        step=step.recordCancellation(CUSTOMER,AT.plusSeconds(2));
        repository.saveCurrent(step);
        step=step.recordRefundRequest(AT.plusSeconds(3));
        repository.saveCurrent(step);
        // 3 lines * 3 workflow occurrences = 9 rows for a two-way JOIN.
        try(var c=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            var ps=c.prepareStatement("""
                SELECT COUNT(*) FROM orders o
                JOIN order_lines l ON l.order_id=o.id
                JOIN order_workflow_occurrences h ON h.order_id=o.id
                WHERE o.id=?""")) {
            ps.setObject(1,ID.value());
            try(var rs=ps.executeQuery()){
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(9);
            }
        }
        var recovered=repository.findCurrentById(ID).orElseThrow();
        assertThat(recovered.lines()).hasSize(3);
        assertThat(recovered.workflowOccurrences()).hasSize(3);
    }
}
