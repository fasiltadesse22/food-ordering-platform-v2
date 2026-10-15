package com.acme.foodordering.adapter.out.postgresql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** V8.0 C1.2-P13: real PostgreSQL row-local integrity and migration data gate.
 * Tests bypass Java constructors intentionally; SQL is the tested authority.
 */
class OrderRelationalIntegrityConstraintsTest {
    private static PostgreSQLContainer pg;
    private static final UUID ID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174231");

    @BeforeAll static void start() {
        pg = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ordering_p13_constraints")
            .withUsername("ordering")
            .withPassword("test-only-password");
        pg.start();
        var r = flyway(pg).migrate();
        assertThat(r.success).isTrue();
        assertThat(r.migrationsExecuted).isEqualTo(3);
    }

    @AfterAll static void stop() {
        if (pg != null) pg.stop();
    }

    @BeforeEach void truncate() throws Exception {
        try (var connection = connection(pg);
             var statement = connection.createStatement()) {
            statement.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
        }
    }

    private static Flyway flyway(PostgreSQLContainer db) {
        return Flyway.configure()
            .dataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())
            .locations("classpath:db/migration").load();
    }
    private static Connection connection(PostgreSQLContainer db) throws SQLException {
        return DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
    }
    private static void execute(PostgreSQLContainer db, String sql) throws SQLException {
        try (var c = connection(db); var statement = c.createStatement()) {
            statement.execute(sql);
        }
    }
    private static void order() throws SQLException {
        execute(pg, "INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at) " +
            "VALUES ('" + ID + "','customer-1','restaurant-1','PLACED'," +
            "'2026-10-12T10:11:12.123456+00')");
    }
    private static void line() throws SQLException {
        execute(pg, "INSERT INTO order_lines(order_id,line_position,menu_item_id,item_name,quantity,unit_price) " +
            "VALUES ('" + ID + "',0,'menu-1','Burger',2,5.50)");
    }
    private static void occurrence() throws SQLException {
        execute(pg, "INSERT INTO order_workflow_occurrences(order_id,occurrence_position,action,participant,occurred_at) " +
            "VALUES ('" + ID + "',0,'RESTAURANT_ACCEPTED','RESTAURANT_OPERATOR'," +
            "'2026-10-12T10:11:13+00')");
    }
    private static void checkFailure(String sql) {
        assertThatThrownBy(() -> execute(pg,sql))
            .isInstanceOf(SQLException.class)
            .satisfies(e -> assertThat(((SQLException)e).getSQLState()).isEqualTo("23514"));
    }

    @Test void validDomainCompatibleRowsPassTheNewConstraints() throws Exception {
        order();line();occurrence();
        execute(pg,"UPDATE order_lines SET unit_price=0 WHERE order_id='" + ID + "'");
        execute(pg,"UPDATE orders SET status='ACCEPTED' WHERE id='" + ID + "'");
        try(var c=connection(pg); var s=c.createStatement();
            var rs=s.executeQuery("SELECT unit_price FROM order_lines WHERE order_id='" + ID + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBigDecimal(1)).isEqualByComparingTo("0");
        }
    }

    @Test void zeroOrNegativeQuantityIsRejectedIndependentlyOfJavaConstructors() throws Exception {
        order();line();
        checkFailure("UPDATE order_lines SET quantity=0 WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_lines SET quantity=-2 WHERE order_id='" + ID + "'");
    }

    @Test void negativeOrNonFiniteNumericPricesAreRejected() throws Exception {
        order();line();
        checkFailure("UPDATE order_lines SET unit_price=-0.01 WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_lines SET unit_price='NaN'::NUMERIC WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_lines SET unit_price='Infinity'::NUMERIC WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_lines SET unit_price='-Infinity'::NUMERIC WHERE order_id='" + ID + "'");
    }

    @Test void blankOrderReferenceIdentifiersAreRejected() throws Exception {
        order();
        checkFailure("UPDATE orders SET customer_id='    ' WHERE id='" + ID + "'");
        checkFailure("UPDATE orders SET restaurant_id='' WHERE id='" + ID + "'");
    }

    @Test void blankLineMenuIdentityOrNameIsRejected() throws Exception {
        order();line();
        checkFailure("UPDATE order_lines SET menu_item_id='  ' WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_lines SET item_name='' WHERE order_id='" + ID + "'");
    }

    @Test void negativeListPositionsAreRejected() throws Exception {
        order();line();occurrence();
        checkFailure("UPDATE order_lines SET line_position=-1 WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_workflow_occurrences SET occurrence_position=-1 WHERE order_id='" + ID + "'");
    }

    @Test void databaseRejectsUnknownOrderLifecycleState() throws Exception {
        order();
        checkFailure("UPDATE orders SET status='DELIVERED' WHERE id='" + ID + "'");
        checkFailure("UPDATE orders SET status='' WHERE id='" + ID + "'");
    }

    @Test void databaseRejectsUnknownOccurrenceActionAndParticipant() throws Exception {
        order();occurrence();
        checkFailure("UPDATE order_workflow_occurrences SET action='FRAUDULENT' WHERE order_id='" + ID + "'");
        checkFailure("UPDATE order_workflow_occurrences SET participant='UNKNOWN_ACTOR' WHERE order_id='" + ID + "'");
    }

    @Test void rejectedMutationDoesNotPartiallyReplaceExistingRow() throws Exception {
        order();line();
        checkFailure("UPDATE order_lines SET quantity=0,unit_price=99 WHERE order_id='" + ID + "'");
        try(var c=connection(pg);var s=c.createStatement();
            var rs=s.executeQuery("SELECT quantity,unit_price FROM order_lines WHERE order_id='" + ID + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
            assertThat(rs.getBigDecimal(2)).isEqualByComparingTo("5.50");
        }
    }

    @Test void databaseStillPermitsBusinessInvalidCompletedWithoutHistoryAsNegativeControl() throws Exception {
        order();line();
        execute(pg,"UPDATE orders SET status='COMPLETED' WHERE id='" + ID + "'");
        // Every individual column is allowed; required history remains absent.
        // P12 reconstitution rejects this. SQL row-local CHECK cannot prove it.
        try(var c=connection(pg);var s=c.createStatement();
            var rs=s.executeQuery("SELECT status FROM orders WHERE id='" + ID + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("COMPLETED");
        }
    }

    @Test void databaseStillPermitsOrderRootWithNoLinesAsNegativeControl() throws Exception {
        order();
        // Foreign keys protect children from orphaning, not parents from
        // having zero children. Domain reconstitution detects empty lines.
        try(var c=connection(pg);var s=c.createStatement();
            var rs=s.executeQuery("SELECT COUNT(*) FROM order_lines WHERE order_id='" + ID + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isZero();
        }
    }

    @Test void v2MigrationRejectsExistingDirtyV1DataThenSucceedsAfterExplicitCleanup() throws Exception {
        // A separate actual PostgreSQL database ensures migration starts at V1
        // and proves V2 validates existing rows rather than only future writes.
        try(var older = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("ordering_existing_v1")
                .withUsername("ordering")
                .withPassword("test-only-password")) {
            older.start();
            var v1=Flyway.configure()
                .dataSource(older.getJdbcUrl(),older.getUsername(),older.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1")).load();
            assertThat(v1.migrate().migrationsExecuted).isEqualTo(1);
            execute(older,"INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at) VALUES ('"
                    + ID + "','customer-1','restaurant-1','PLACED','2026-10-12T10:00:00+00')");
            execute(older,"INSERT INTO order_lines(order_id,line_position,menu_item_id,item_name,quantity,unit_price) VALUES ('"
                    + ID + "',0,'menu-1','Burger',0,-2.00)");

            assertThatThrownBy(() -> flyway(older).migrate())
                    .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
            // This is explicit operational remediation, NOT silent cleanup by
            // the migration or an assumption that invalid history never exists.
            execute(older,"UPDATE order_lines SET quantity=1,unit_price=0 WHERE order_id='" + ID + "'");
            var repaired=flyway(older).migrate();
            assertThat(repaired.success).isTrue();
            assertThat(repaired.migrationsExecuted).isEqualTo(1);
            assertThat(flyway(older).info().applied()).hasSize(3);
            assertThatThrownBy(() -> execute(older,
                "UPDATE order_lines SET quantity=0 WHERE order_id='" + ID + "'"))
                .isInstanceOf(SQLException.class)
                .satisfies(e -> assertThat(((SQLException)e).getSQLState()).isEqualTo("23514"));
        }
    }
}
