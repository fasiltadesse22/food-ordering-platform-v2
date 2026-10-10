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

class OrderConcurrentConflictWindowTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T05:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC);
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void sequentialAcceptThenRejectUsesFreshAuthorityAndRejectsSecondDecision() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);

        var service = new OrderWorkflowService(repository, CLOCK);

        var accept = service.recordRestaurantAcceptance(
                placed.id(),
                RESTAURANT
        );
        var reject = service.recordRestaurantRejection(
                placed.id(),
                RESTAURANT
        );

        assertThat(accept)
                .isInstanceOf(OrderActionResult.Accepted.class);

        assertThat(reject)
                .isInstanceOfSatisfying(
                        OrderActionResult.Rejected.class,
                        result -> assertThat(result.rejection().code())
                                .isEqualTo(
                                        OrderActionRejection.Code
                                                .ILLEGAL_TRANSITION
                                )
                );

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
    void twoLocallyValidDecisionsFromSamePlacedSnapshotCanBothReturnAcceptedWhileLastSaveWins() throws Exception {
        var repository = new CoordinatedConflictRepository(
                placedOrder(),
                OrderStatus.ACCEPTED
        );
        var service = new OrderWorkflowService(repository, CLOCK);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var acceptFuture = executor.submit(
                    () -> service.recordRestaurantAcceptance(
                            repository.orderId(),
                            RESTAURANT
                    )
            );
            var rejectFuture = executor.submit(
                    () -> service.recordRestaurantRejection(
                            repository.orderId(),
                            RESTAURANT
                    )
            );

            var acceptResult = acceptFuture.get(5, TimeUnit.SECONDS);
            var rejectResult = rejectFuture.get(5, TimeUnit.SECONDS);

            assertAcceptedWithStatus(acceptResult, "ACCEPTED");
            assertAcceptedWithStatus(rejectResult, "REJECTED");
        }

        assertThat(repository.readStatuses())
                .containsExactly(
                        OrderStatus.PLACED,
                        OrderStatus.PLACED
                );
        assertThat(repository.saveStatuses())
                .containsExactly(
                        OrderStatus.ACCEPTED,
                        OrderStatus.REJECTED
                );

        var authoritative =
                repository.findCurrentWithoutCoordination();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.REJECTED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.RESTAURANT_REJECTED
                );
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(
                        OrderWorkflowAction.RESTAURANT_ACCEPTED
                );
    }

    @Test
    void reversingOnlyTheSaveOrderReversesWhichAcceptedDecisionSurvives() throws Exception {
        var repository = new CoordinatedConflictRepository(
                placedOrder(),
                OrderStatus.REJECTED
        );
        var service = new OrderWorkflowService(repository, CLOCK);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var acceptFuture = executor.submit(
                    () -> service.recordRestaurantAcceptance(
                            repository.orderId(),
                            RESTAURANT
                    )
            );
            var rejectFuture = executor.submit(
                    () -> service.recordRestaurantRejection(
                            repository.orderId(),
                            RESTAURANT
                    )
            );

            var acceptResult = acceptFuture.get(5, TimeUnit.SECONDS);
            var rejectResult = rejectFuture.get(5, TimeUnit.SECONDS);

            assertAcceptedWithStatus(acceptResult, "ACCEPTED");
            assertAcceptedWithStatus(rejectResult, "REJECTED");
        }

        assertThat(repository.readStatuses())
                .containsExactly(
                        OrderStatus.PLACED,
                        OrderStatus.PLACED
                );
        assertThat(repository.saveStatuses())
                .containsExactly(
                        OrderStatus.REJECTED,
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
                        OrderWorkflowAction.RESTAURANT_REJECTED
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

    private static Order placedOrder() {
        return Order.place(
                OrderId.from(
                        "123e4567-e89b-12d3-a456-426614174013"
                ),
                new CustomerId("customer-1"),
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
     * Test-only repository that forces both operations to read the same
     * authoritative snapshot before either save is allowed to become current.
     *
     * The constructor also selects which successor must save first, making the
     * interleaving deterministic rather than scheduler-dependent.
     */
    private static final class CoordinatedConflictRepository
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

        private CoordinatedConflictRepository(
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
                    "both operations did not reach the read barrier"
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
                    "selected first save did not complete"
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
                String timeoutMessage
        ) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(timeoutMessage);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while coordinating conflict",
                        exception
                );
            }
        }
    }
}
