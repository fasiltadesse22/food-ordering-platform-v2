package com.acme.foodordering.adapter.out.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class OrderRelationalSchemaMigrationTest {
  private static PostgreSQLContainer pg;
  private static Flyway flyway;
  private static final UUID A=UUID.fromString("123e4567-e89b-12d3-a456-426614174211");
  private static final UUID B=UUID.fromString("123e4567-e89b-12d3-a456-426614174212");
  private static final Instant T=Instant.parse("2026-10-12T12:12:13.123456Z");

  @BeforeAll static void startAndMigrate() {
    pg=new PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("ordering_schema")
        .withUsername("ordering")
        .withPassword("test-only-password");
    pg.start();
    flyway=Flyway.configure().dataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword())
        .locations("classpath:db/migration").load();
    var result=flyway.migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);
  }
  @AfterAll static void close() {if(pg!=null)pg.stop();}
  @BeforeEach void truncate() throws SQLException {
    try(var db=open();var stmt=db.createStatement()) {
      stmt.execute("TRUNCATE order_workflow_occurrences, order_lines, orders");
    }
  }
  private static Connection open() throws SQLException {
    return DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
  }
  private static void root(Connection db,UUID id) throws SQLException {
    try(var ps=db.prepareStatement(
        "INSERT INTO orders(id,customer_id,restaurant_id,status,placed_at) VALUES(?,?,?,?,?)")) {
      ps.setObject(1,id);
      ps.setString(2,"customer-1");
      ps.setString(3,"restaurant-1");
      ps.setString(4,"PLACED");
      ps.setObject(5,OffsetDateTime.ofInstant(T,ZoneOffset.UTC));
      ps.executeUpdate();
    }
  }
  private static void line(Connection db,UUID id,int position,int quantity,BigDecimal price)
      throws SQLException {
    try(var ps=db.prepareStatement("INSERT INTO order_lines(order_id,line_position,"
        +"menu_item_id,item_name,quantity,unit_price) VALUES(?,?,?,?,?,?)")) {
      ps.setObject(1,id);ps.setInt(2,position);ps.setString(3,"burger-1");
      ps.setString(4,"Burger");ps.setInt(5,quantity);ps.setBigDecimal(6,price);
      ps.executeUpdate();
    }
  }
  private static void occurrence(Connection db,UUID id,int pos,String action) throws SQLException {
    try(var ps=db.prepareStatement("INSERT INTO order_workflow_occurrences("
      +"order_id,occurrence_position,action,participant,occurred_at) VALUES(?,?,?,?,?)")) {
      ps.setObject(1,id);ps.setInt(2,pos);ps.setString(3,action);
      ps.setString(4,"RESTAURANT_OPERATOR");
      ps.setObject(5,OffsetDateTime.ofInstant(T.plusSeconds(pos),ZoneOffset.UTC));
      ps.executeUpdate();
    }
  }
  private static void sqlState(ThrowingSqlAction action,String expected) {
    assertThatThrownBy(action::run)
      .isInstanceOf(SQLException.class)
      .satisfies(e->assertThat(((SQLException)e).getSQLState()).isEqualTo(expected));
  }
  @FunctionalInterface interface ThrowingSqlAction {void run() throws SQLException;}

  @Test void flywayV1AndV2RunOnceAndRepeatMigrationIsNoop() {
    var result=flyway.migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isZero();
    assertThat(flyway.info().applied()).hasSize(2);
  }
  @Test void allThreeRealPostgresTablesExist() throws SQLException {
    try(var db=open();var ps=db.prepareStatement("SELECT table_name FROM information_schema.tables "
      +"WHERE table_schema='public' AND table_name IN "
      +"('orders','order_lines','order_workflow_occurrences') ORDER BY table_name");
      var rs=ps.executeQuery()) {
      var names=new java.util.ArrayList<String>();
      while(rs.next())names.add(rs.getString(1));
      assertThat(names).containsExactly("order_lines","order_workflow_occurrences","orders");
    }
  }
  @Test void uniqueAggregateUuidIsEnforcedByDatabase() throws SQLException {
    try(var db=open()) {
      root(db,A);
      sqlState(()->root(db,A),"23505");
      try(var ps=db.prepareStatement("SELECT COUNT(*) FROM orders WHERE id=?")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {rs.next();assertThat(rs.getInt(1)).isEqualTo(1);}
      }
    }
  }
  @Test void orphanChildrenAreRejectedByPostgresForeignKeys() throws SQLException {
    try(var db=open()) {
      sqlState(()->line(db,A,0,1,new BigDecimal("5.50")),"23503");
      sqlState(()->occurrence(db,A,0,"RESTAURANT_ACCEPTED"),"23503");
    }
  }
  @Test void childPositionUniquenessAndExplicitOrderArePreserved() throws SQLException {
    try(var db=open()) {
      root(db,A);root(db,B);
      line(db,A,1,1,new BigDecimal("4.00"));
      line(db,A,0,2,new BigDecimal("5.50"));
      line(db,B,0,1,new BigDecimal("9.00"));
      occurrence(db,A,1,"PREPARATION_STARTED");
      occurrence(db,A,0,"RESTAURANT_ACCEPTED");
      sqlState(()->line(db,A,0,1,new BigDecimal("1.00")),"23505");
      sqlState(()->occurrence(db,A,0,"ORDER_COMPLETED"),"23505");
      try(var ps=db.prepareStatement("SELECT line_position FROM order_lines "
          +"WHERE order_id=? ORDER BY line_position")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {
          assertThat(rs.next()).isTrue();assertThat(rs.getInt(1)).isZero();
          assertThat(rs.next()).isTrue();assertThat(rs.getInt(1)).isEqualTo(1);
          assertThat(rs.next()).isFalse();
        }
      }
      try(var ps=db.prepareStatement("SELECT action FROM order_workflow_occurrences "
          +"WHERE order_id=? ORDER BY occurrence_position")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {
          assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).isEqualTo("RESTAURANT_ACCEPTED");
          assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).isEqualTo("PREPARATION_STARTED");
          assertThat(rs.next()).isFalse();
        }
      }
    }
  }
  @Test void unconstrainedNumericPreservesArbitraryBigDecimalPrecision() throws SQLException {
    try(var db=open()) {
      root(db,A);
      var amount=new BigDecimal("123456789012345.123456789");
      line(db,A,0,2,amount);
      try(var ps=db.prepareStatement("SELECT unit_price FROM order_lines "
          +"WHERE order_id=? AND line_position=0")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getBigDecimal(1)).isEqualByComparingTo(amount);
        }
      }
    }
  }
  @Test void placedAtRoundTripsAsUtcInstantAtMicrosecondPrecision() throws SQLException {
    try(var db=open()) {
      root(db,A);
      try(var ps=db.prepareStatement("SELECT placed_at FROM orders WHERE id=?")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getObject(1,OffsetDateTime.class).toInstant()).isEqualTo(T);
        }
      }
    }
  }
  @Test void v2DatabaseNowRejectsP11ZeroQuantityAndNegativePrice() throws SQLException {
    try(var db=open()) {
      root(db,A);
      sqlState(()->line(db,A,0,0,new BigDecimal("-2.00")),"23514");
      sqlState(()->line(db,A,0,0,new BigDecimal("2.00")),"23514");
      sqlState(()->line(db,A,0,1,new BigDecimal("-2.00")),"23514");
      try(var ps=db.prepareStatement("SELECT COUNT(*) FROM order_lines WHERE order_id=?")) {
        ps.setObject(1,A);
        try(var rs=ps.executeQuery()) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getInt(1)).isZero();
        }
      }
    }
    // Before-state preserved in P11 checkpoint; V2 now prevents invalid writes.
  }
}
