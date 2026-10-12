package com.acme.foodordering.domain.order;

import com.acme.foodordering.application.service.OrderSnapshot;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class OrderValueSemanticsContractTest {
    private static final Instant AT=Instant.parse("2026-10-12T10:00:00Z");
    private static final CustomerId CUSTOMER=new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-1");

    private static OrderLine line(String price) {
        return new OrderLine("burger-1","Classic Burger",2,new BigDecimal(price));
    }
    private static Order placed(List<OrderLine> lines) {
        return Order.place(OrderId.from("123e4567-e89b-12d3-a456-426614174091"),
                CUSTOMER,RESTAURANT,lines,AT);
    }

    @Test void equalOrderLineValuesHaveValueEqualityAndAreInterchangeable() {
        var a=line("5.50"); var b=line("5.50");
        assertThat(a).isNotSameAs(b).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.lineTotal()).isEqualByComparingTo("11.00");
    }

    @Test void numericMonetaryEqualityAndRecordEqualityAreDifferent() {
        var a=line("5.0"); var b=line("5.00");
        assertThat(a.unitPrice().compareTo(b.unitPrice())).isZero();
        assertThat(a.unitPrice().equals(b.unitPrice())).isFalse();
        assertThat(a.equals(b)).isFalse();
        assertThat(a.lineTotal().compareTo(b.lineTotal())).isZero();
        assertThat(a.lineTotal().equals(b.lineTotal())).isFalse();
    }

    @Test void invalidComponentsCannotCreateAnOrderLine() {
        assertThatThrownBy(()->new OrderLine(" ","Burger",2,new BigDecimal("5.50")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OrderLine("burger-1"," ",2,new BigDecimal("5.50")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OrderLine("burger-1","Burger",0,new BigDecimal("5.50")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OrderLine("burger-1","Burger",1,new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OrderLine("burger-1","Burger",1,null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test void orderDefensivelyCopiesCallerCollectionAndExposesImmutableList() {
        var callerLines=new ArrayList<>(List.of(line("5.50")));
        var order=placed(callerLines);
        callerLines.add(new OrderLine("fries-1","Fries",1,new BigDecimal("3.00")));
        assertThat(order.lines()).hasSize(1);
        assertThatThrownBy(()->order.lines().add(line("2.00")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.total()).isEqualByComparingTo("11.00");
    }

    @Test void modifyingLinesReturnsNewOrderWithoutMutatingOldValues() {
        var original=placed(List.of(line("5.50")));
        var replacement=new ArrayList<>(List.of(line("7.00")));
        var modified=original.modifyLines(CUSTOMER,replacement,AT.plusSeconds(1));
        replacement.clear();
        assertThat(modified).isNotSameAs(original);
        assertThat(modified.id()).isEqualTo(original.id());
        assertThat(original.total()).isEqualByComparingTo("11.00");
        assertThat(modified.total()).isEqualByComparingTo("14.00");
        assertThat(modified.lines()).hasSize(1);
        assertThat(modified.workflowOccurrences()).hasSize(1);
    }

    @Test void snapshotDtoCopiesValuesButIsNotCurrentDomainAuthority() {
        var original=placed(List.of(line("5.50")));
        var snapshot=OrderSnapshot.from(original);
        var modified=original.modifyLines(CUSTOMER,List.of(line("7.00")),AT.plusSeconds(1));
        assertThat(snapshot.total()).isEqualByComparingTo("11.00");
        assertThat(modified.total()).isEqualByComparingTo("14.00");
        assertThatThrownBy(()->snapshot.lines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshot.lines()).hasSize(1);
    }
}
