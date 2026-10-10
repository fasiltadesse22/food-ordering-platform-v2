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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepeatedCommandReplayTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T08:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC);
    private static final CustomerId CUSTOMER =
            new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void lostCancellationSuccessFollowedByRetryConvergesStateButDoesNotReplayOriginalSuccessResult() {
        var repository = new CountingRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);

        var service = new OrderWorkflowService(repository, CLOCK);

        var firstResult = service.recordCancellation(
                placed.id(),
                CUSTOMER
        );

        // Model a lost response by deliberately discarding the first result
        // before issuing the same logical intent again.
        assertAcceptedWithStatus(firstResult, "CANCELLED");

        var retryResult = service.recordCancellation(
                placed.id(),
                CUSTOMER
        );

        assertThat(retryResult)
                .isInstanceOfSatisfying(
                        OrderActionResult.Rejected.class,
                        rejected -> assertThat(
                                rejected.rejection().code()
                        ).isEqualTo(
                                OrderActionRejection.Code
                                        .ILLEGAL_TRANSITION
                        )
                );

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);

        // Initial seed + first successful cancellation.
        // The replay is rejected before another save.
        assertThat(repository.saveCount()).isEqualTo(2);
    }

    @Test
    void concurrentIdenticalCancellationsCanBothExecuteAndReturnAcceptedWithoutBeingDeduplicated() throws Exception {
        var repository =
                new CoordinatedDuplicateCancellationRepository(
                        placedOrder()
                );
        var service = new OrderWorkflowService(repository, CLOCK);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(
                    () -> service.recordCancellation(
                            repository.orderId(),
                            CUSTOMER
                    )
            );
            var second = executor.submit(
                    () -> service.recordCancellation(
                            repository.orderId(),
                            CUSTOMER
                    )
            );

            assertAcceptedWithStatus(
                    first.get(5, TimeUnit.SECONDS),
                    "CANCELLED"
            );
            assertAcceptedWithStatus(
                    second.get(5, TimeUnit.SECONDS),
                    "CANCELLED"
            );
        }

        assertThat(repository.readCount()).isEqualTo(2);
        assertThat(repository.saveCount()).isEqualTo(2);

        var authoritative =
                repository.findCurrentWithoutCoordination();

        assertThat(authoritative.status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.ORDER_CANCELLED);
    }

    @Test
    void repeatedRefundRequestIsExplicitlySuppressedButDoesNotReplayTheFirstAcceptedOutcome() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var service = new OrderWorkflowService(repository, CLOCK);

        assertAcceptedWithStatus(
                service.recordPayment(placed.id()),
                "PLACED"
        );
        assertAcceptedWithStatus(
                service.recordCancellation(placed.id(), CUSTOMER),
                "CANCELLED"
        );

        var firstRefund =
                service.recordRefundRequest(placed.id());
        var replayedRefund =
                service.recordRefundRequest(placed.id());

        assertAcceptedWithStatus(firstRefund, "CANCELLED");

        assertThat(replayedRefund)
                .isInstanceOfSatisfying(
                        OrderActionResult.Rejected.class,
                        rejected -> assertThat(
                                rejected.rejection().code()
                        ).isEqualTo(
                                OrderActionRejection.Code
                                        .REFUND_ALREADY_REQUESTED
                        )
                );

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.ORDER_CANCELLED,
                        OrderWorkflowAction.REFUND_REQUESTED
                );
    }

    private static void assertAcceptedWithStatus(
            OrderActionResult result,
            String status
    ) {
        assertThat(result)
                .isInstanceOfSatisfying(
                        OrderActionResult.Accepted.class,
                        accepted -> assertThat(
                                accepted.order().status()
                        ).isEqualTo(status)
                );
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from(
                        "123e4567-e89b-12d3-a456-426614174015"
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

    private static final class CountingRepository
            implements OrderRepository {

        private final InMemoryOrderRepository delegate =
                new InMemoryOrderRepository();
        private final AtomicInteger saveCount =
                new AtomicInteger();

        @Override
        public Optional<Order> findCurrentById(OrderId orderId) {
            return delegate.findCurrentById(orderId);
        }

        @Override
        public void saveCurrent(Order order) {
            delegate.saveCurrent(order);
            saveCount.incrementAndGet();
        }

        private int saveCount() {
            return saveCount.get();
        }
    }

    /**
     * Test-only harness proving that identical concurrent intent is not
     * deduplicated merely because the final immutable Order converges.
     */
    private static final class CoordinatedDuplicateCancellationRepository
            implements OrderRepository {

        private volatile Order current;
        private final CountDownLatch bothReads =
                new CountDownLatch(2);
        private final AtomicInteger readCount =
                new AtomicInteger();
        private final AtomicInteger saveCount =
                new AtomicInteger();

        private CoordinatedDuplicateCancellationRepository(
                Order initial
        ) {
            current = initial;
        }

        @Override
        public Optional<Order> findCurrentById(OrderId orderId) {
            var snapshot = current;
            readCount.incrementAndGet();
            bothReads.countDown();
            await(bothReads);
            return Optional.of(snapshot);
        }

        @Override
        public void saveCurrent(Order order) {
            current = order;
            saveCount.incrementAndGet();
        }

        private OrderId orderId() {
            return current.id();
        }

        private int readCount() {
            return readCount.get();
        }

        private int saveCount() {
            return saveCount.get();
        }

        private Order findCurrentWithoutCoordination() {
            return current;
        }

        private static void await(CountDownLatch latch) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "both duplicate requests did not capture the shared snapshot"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while coordinating duplicate cancellation",
                        exception
                );
            }
        }
    }
}
