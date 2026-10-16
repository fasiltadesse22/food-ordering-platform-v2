package com.acme.foodordering.domain.inventory;

import com.acme.foodordering.domain.order.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** P21 pure-policy tests; no claim of durable allocation. */
class OrderInventoryReservationPolicyTest {
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-21");
    private static final CustomerId CUSTOMER=new CustomerId("customer-21");
    private static final OrderId ID=OrderId.from("123e4567-e89b-12d3-a456-426614174321");
    private static final Instant AT=Instant.parse("2026-10-16T12:00:00Z");
    private static Order order(OrderLine... lines) {
        return Order.place(ID,CUSTOMER,RESTAURANT,List.of(lines),AT);
    }
    private static OrderLine burger(int n) {
        return new OrderLine("burger","Burger",n,new BigDecimal("5.00"));
    }
    private static OrderLine water(int n) {
        return new OrderLine("water","Water",n,new BigDecimal("1.00"));
    }
    @Test void duplicateMenuIdentifiersAreSummedBeforeAvailabilityDecision(){
        var order=order(burger(1),burger(2),water(1));
        var snapshot=new InventoryAvailabilitySnapshot(RESTAURANT,7,
                Map.of("burger",3,"water",2));
        var plan=new OrderInventoryReservationPolicy().propose(order,snapshot);
        assertThat(plan.orderId()).isEqualTo(ID);
        assertThat(plan.restaurantId()).isEqualTo(RESTAURANT);
        assertThat(plan.observedInventoryVersion()).isEqualTo(7);
        assertThat(plan.requestedByMenuItem()).containsExactlyInAnyOrderEntriesOf(
                Map.of("burger",3,"water",1));
        assertThat(order.version()).isZero();
        assertThat(order.workflowOccurrences()).isEmpty();
    }
    @Test void combinedDemandCannotBeHiddenByIndividuallySmallLines(){
        var order=order(burger(1),burger(2));
        var snapshot=new InventoryAvailabilitySnapshot(RESTAURANT,0,Map.of("burger",2));
        assertThatThrownBy(()->new OrderInventoryReservationPolicy().propose(order,snapshot))
            .isInstanceOfSatisfying(InventoryReservationPlanningException.class,
                e->assertThat(e.code()).isEqualTo(
                    InventoryReservationPlanningException.Code.INSUFFICIENT_OBSERVED_QUANTITY));
    }
    @Test void wrongRestaurantSnapshotIsNotInterchangeable(){
        var order=order(burger(1));
        var snapshot=new InventoryAvailabilitySnapshot(
                new RestaurantId("other"),0,Map.of("burger",10));
        assertThatThrownBy(()->new OrderInventoryReservationPolicy().propose(order,snapshot))
            .isInstanceOfSatisfying(InventoryReservationPlanningException.class,
                e->assertThat(e.code()).isEqualTo(
                    InventoryReservationPlanningException.Code.WRONG_RESTAURANT));
    }
    @Test void absentInventoryItemIsExplicitlyDistinguishedFromInsufficientStock(){
        var order=order(burger(1));
        var snapshot=new InventoryAvailabilitySnapshot(RESTAURANT,0,Map.of("water",10));
        assertThatThrownBy(()->new OrderInventoryReservationPolicy().propose(order,snapshot))
            .isInstanceOfSatisfying(InventoryReservationPlanningException.class,
                e->assertThat(e.code()).isEqualTo(
                    InventoryReservationPlanningException.Code.ITEM_NOT_IN_SNAPSHOT));
    }
    @Test void nonPlacedOrderIsNotEligibleForThisPlanningOperation(){
        var order=order(burger(1)).recordRestaurantAcceptance(RESTAURANT,AT.plusSeconds(1));
        var snapshot=new InventoryAvailabilitySnapshot(RESTAURANT,0,Map.of("burger",2));
        assertThatThrownBy(()->new OrderInventoryReservationPolicy().propose(order,snapshot))
            .isInstanceOfSatisfying(InventoryReservationPlanningException.class,
                e->assertThat(e.code()).isEqualTo(
                    InventoryReservationPlanningException.Code.ORDER_NOT_PLACED));
    }
    @Test void observedAvailabilityAndPlanAreImmutableSnapshotsNotLiveReservations(){
        var mutable=new HashMap<String,Integer>();mutable.put("burger",2);
        var snapshot=new InventoryAvailabilitySnapshot(RESTAURANT,3,mutable);
        mutable.put("burger",0);
        assertThat(snapshot.availableByMenuItem()).containsEntry("burger",2);
        var plan=new OrderInventoryReservationPolicy().propose(order(burger(2)),snapshot);
        assertThatThrownBy(()->plan.requestedByMenuItem().put("burger",9))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(plan.observedInventoryVersion()).isEqualTo(3);
    }
}
