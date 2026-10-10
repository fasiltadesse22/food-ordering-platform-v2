package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionRejection;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.OrderStatus;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCancellationAcceptanceRaceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T07:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC);
    private static final CustomerId CUSTOMER =
            new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void sequentialCancellationThenAcceptanceRejectsRestaurantAgainstFreshCancelledAuthority() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var cancellation = service.recordCancellation(
                placed.id(),
                CUSTOMER
        );
        var acceptance = service.recordRestaurantAcceptance(
                placed.id(),
                RESTAURANT
        );

        assertAcceptedWithStatus(cancellation, "CANCELLED");
        assertIllegalTransition(acceptance);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test
    void sequentialAcceptanceThenCancellationRejectsCustomerAgainstFreshAcceptedAuthority() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        var acceptance = service.recordRestaurantAcceptance(
                placed.id(),
                RESTAURANT
        );
        var cancellation = service.recordCancellation(
                placed.id(),
                CUSTOMER
        );

        assertAcceptedWithStatus(acceptance, "ACCEPTED");
        assertIllegalTransition(cancellation);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.ACCEPTED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
    }

    @Test
    void sharedPlacedSnapshotCanAcknowledgeBothActorsWhileAcceptanceOverwritesEarlierCancellation() throws Exception {
        var repository = new CoordinatedCancellationAcceptanceRepository(
                placedOrder(),
                OrderStatus.CANCELLED
        );
        var service = new OrderWorkflowService(repository, CLOCK);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var cancellationFuture = executor.submit(
                    () -> service.recordCancellation(
                            repository.orderId(),
                            CUSTOMER
                    )
            );
            var acceptanceFuture = executor.submit(
                    () -> service.recordRestaurantAcceptance(
                            repository.orderId(),
                            RESTAURANT
                    )
            );

            var cancellation =
                    cancellationFuture.get(5, TimeUnit.SECONDS);
            var acceptance =
                    acceptanceFuture.get(5, TimeUnit.SECONDS);

            assertAcceptedWithStatus(cancellation, "CANCELLED");
            assertAcceptedWithStatus(acceptance, "ACCEPTED");
        }

        assertThat(repository.readStatuses())
                .containsExactly(
                        OrderStatus.PLACED,
                        OrderStatus.PLACED
                );

        assertThat(repository.saveStatuses())
                .containsExactly(
                        OrderStatus.CANCELLED,
                        OrderStatus.ACCEPTED
                );

        var authoritative =
                repository.findCurrentWithoutCoordination();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.ACCEPTED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(
                        OrderWorkflowAction.ORDER_CANCELLED
                );
    }

    @Test
    void reversingOnlySaveOrderLetsCancellationOverwriteEarlierAcceptanceWhileBothActorsWereAcknowledged() throws Exception {
        var repository = new CoordinatedCancellationAcceptanceRepository(
                placedOrder(),
                OrderStatus.ACCEPTED
        );
        var service = new OrderWorkflowService(repository, CLOCK);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var cancellationFuture = executor.submit(
                    () -> service.recordCancellation(
                            repository.orderId(),
                            CUSTOMER
                    )
            );
            var acceptanceFuture = executor.submit(
                    () -> service.recordRestaurantAcceptance(
                            repository.orderId(),
                            RESTAURANT
                    )
            );

            var cancellation =
                    cancellationFuture.get(5, TimeUnit.SECONDS);
            var acceptance =
                    acceptanceFuture.get(5, TimeUnit.SECONDS);

            assertAcceptedWithStatus(cancellation, "CANCELLED");
            assertAcceptedWithStatus(acceptance, "ACCEPTED");
        }

        assertThat(repository.readStatuses())
                .containsExactly(
                        OrderStatus.PLACED,
                        OrderStatus.PLACED
                );

        assertThat(repository.saveStatuses())
                .containsExactly(
                        OrderStatus.ACCEPTED,
                        OrderStatus.CANCELLED
                );

        var authoritative =
                repository.findCurrentWithoutCoordination();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
    }

    private static void assertAcceptedWithStatus(
            OrderActionResult result,
            String expectedStatus
    ) {
        assertThat(result)
                .isInstanceOfSatisfying(
                        OrderActionResult.Accepted.class,
                        accepted -> assertThat(
                                accepted.order().status()
                        ).isEqualTo(expectedStatus)
                );
    }

    private static void assertIllegalTransition(
            OrderActionResult result
    ) {
        assertThat(result)
                .isInstanceOfSatisfying(
                        OrderActionResult.Rejected.class,
                        rejected -> assertThat(
                                rejected.rejection().code()
                        ).isEqualTo(
                                OrderActionRejection.Code
                                        .ILLEGAL_TRANSITION
                        )
                );
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from(
                        "123e4567-e89b-12d3-a456-426614174014"
                ),
                CUSTOMER,
                RESTAURANT,
                List.of(new OrderLine(
                        "burger-1",
                        "Classic Burger",
                        1,
                        new BigDecimal("5.50")
                )),
                NOW
        );
    }

    /**
     * Test-only conflict harness.
     *
     * Both actors capture the same PLACED authority before either save can
     * proceed. The selected first-save status makes the business interleaving
     * deterministic while leaving production code untouched.
     */
    private static final class CoordinatedCancellationAcceptanceRepository
            implements OrderRepository {

        private volatile Order current;
        private final OrderStatus firstSaveStatus;
        private final CountDownLatch bothReads =
                new CountDownLatch(2);
        private final CountDownLatch firstSaveCompleted =
                new CountDownLatch(1);
        private final List<OrderStatus> readStatuses =
                new CopyOnWriteArrayList<>();
        private final List<OrderStatus> saveStatuses =
                new CopyOnWriteArrayList<>();

        private CoordinatedCancellationAcceptanceRepository(
                Order initial,
                OrderStatus firstSaveStatus
        ) {
            this.current = initial;
            this.firstSaveStatus = firstSaveStatus;
        }

        @Override
        public Optional<Order> findCurrentById(OrderId orderId) {
            var snapshot = current;
            readStatuses.add(snapshot.status());
            bothReads.countDown();
            await(
                    bothReads,
                    "both actors did not capture the shared Order snapshot"
            );
            return Optional.of(snapshot);
        }

        @Override
        public void saveCurrent(Order order) {
            if (order.status() == firstSaveStatus) {
                current = order;
                saveStatuses.add(order.status());
                firstSaveCompleted.countDown();
                return;
            }

            await(
                    firstSaveCompleted,
                    "selected first business decision did not save"
            );
            current = order;
            saveStatuses.add(order.status());
        }

        private OrderId orderId() {
            return current.id();
        }

        private List<OrderStatus> readStatuses() {
            return List.copyOf(readStatuses);
        }

        private List<OrderStatus> saveStatuses() {
            return List.copyOf(saveStatuses);
        }

        private Order findCurrentWithoutCoordination() {
            return current;
        }

        private static void await(
                CountDownLatch latch,
                String message
        ) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(message);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while coordinating cancellation/acceptance",
                        exception
                );
            }
        }
    }
}
