package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.CustomerId;
import com.acme.foodordering.domain.order.Order;
import com.acme.foodordering.domain.order.OrderId;
import com.acme.foodordering.domain.order.OrderLine;
import com.acme.foodordering.domain.order.RestaurantId;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDuplicatePaymentExternalEffectTest {

    private static final Instant NOW =
            Instant.parse("2026-10-10T09:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC);
    private static final CustomerId CUSTOMER =
            new CustomerId("customer-1");
    private static final RestaurantId RESTAURANT =
            new RestaurantId("restaurant-1");

    @Test
    void repeatedLocalPaymentCommandCurrentlyRecordsDuplicatePaymentFacts() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);
        var workflow = new OrderWorkflowService(repository, CLOCK);

        var first = workflow.recordPayment(placed.id());
        var second = workflow.recordPayment(placed.id());

        assertAccepted(first);
        assertAccepted(second);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.PAYMENT_RECORDED
                );
    }

    @Test
    void chargeThenRecordWithoutReplayIdentityCanDuplicateBothExternalChargeAndLocalPaymentFact() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);

        var workflow = new OrderWorkflowService(repository, CLOCK);
        var provider = new RecordingPaymentAuthority();
        var coordinator = new ChargeThenRecordCoordinator(
                workflow,
                provider
        );

        assertAccepted(coordinator.pay(placed.id(), placed.total()));
        assertAccepted(coordinator.pay(placed.id(), placed.total()));

        assertThat(provider.successfulCharges()).isEqualTo(2);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(
                        OrderWorkflowAction.PAYMENT_RECORDED,
                        OrderWorkflowAction.PAYMENT_RECORDED
                );
    }

    @Test
    void providerCanCommitChargeThenLoseResponseSoRetryChargesTwiceWhileLocalOrderRecordsPaymentOnce() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);

        var workflow = new OrderWorkflowService(repository, CLOCK);
        var provider = new ChargeThenLoseFirstResponseAuthority();
        var coordinator = new ChargeThenRecordCoordinator(
                workflow,
                provider
        );

        assertThatThrownBy(
                () -> coordinator.pay(placed.id(), placed.total())
        )
                .isInstanceOf(PaymentResponseLostException.class);

        var afterUnknownOutcome =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(provider.successfulCharges()).isEqualTo(1);
        assertThat(afterUnknownOutcome.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .doesNotContain(OrderWorkflowAction.PAYMENT_RECORDED);

        var retry = coordinator.pay(placed.id(), placed.total());

        assertAccepted(retry);
        assertThat(provider.successfulCharges()).isEqualTo(2);

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
    }

    @Test
    void recordThenChargeReversesTheFailureWindowAndCanLeaveLocalPaymentFactWithoutExternalCharge() {
        var repository = new InMemoryOrderRepository();
        var placed = placedOrder();
        repository.saveCurrent(placed);

        var workflow = new OrderWorkflowService(repository, CLOCK);
        var provider = new FailBeforeChargeAuthority();
        var coordinator = new RecordThenChargeCoordinator(
                workflow,
                provider
        );

        assertThatThrownBy(
                () -> coordinator.pay(placed.id(), placed.total())
        )
                .isInstanceOf(PaymentProviderUnavailableException.class);

        assertThat(provider.successfulCharges()).isZero();

        var authoritative =
                repository.findCurrentById(placed.id()).orElseThrow();

        assertThat(authoritative.workflowOccurrences())
                .extracting(occurrence -> occurrence.action())
                .containsExactly(OrderWorkflowAction.PAYMENT_RECORDED);
    }

    private static void assertAccepted(OrderActionResult result) {
        assertThat(result)
                .isInstanceOf(OrderActionResult.Accepted.class);
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.from(
                        "123e4567-e89b-12d3-a456-426614174016"
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
     * Test-only model of an authority outside Order current-state authority.
     *
     * A successful charge is treated as independently committed and cannot be
     * erased by replacing the local Order object.
     */
    private interface PaymentAuthority {
        void charge(OrderId orderId, BigDecimal amount);
        int successfulCharges();
    }

    private static final class RecordingPaymentAuthority
            implements PaymentAuthority {

        private final AtomicInteger charges = new AtomicInteger();

        @Override
        public void charge(OrderId orderId, BigDecimal amount) {
            charges.incrementAndGet();
        }

        @Override
        public int successfulCharges() {
            return charges.get();
        }
    }

    /**
     * First call commits the external effect and then loses the response.
     * From the caller/coordinator perspective the outcome is unknown even
     * though the authority has already counted the charge.
     */
    private static final class ChargeThenLoseFirstResponseAuthority
            implements PaymentAuthority {

        private final AtomicInteger charges = new AtomicInteger();
        private final AtomicInteger attempts = new AtomicInteger();

        @Override
        public void charge(OrderId orderId, BigDecimal amount) {
            var attempt = attempts.incrementAndGet();
            charges.incrementAndGet();

            if (attempt == 1) {
                throw new PaymentResponseLostException(
                        "charge committed but provider response was lost"
                );
            }
        }

        @Override
        public int successfulCharges() {
            return charges.get();
        }
    }

    private static final class FailBeforeChargeAuthority
            implements PaymentAuthority {

        @Override
        public void charge(OrderId orderId, BigDecimal amount) {
            throw new PaymentProviderUnavailableException(
                    "provider failed before committing charge"
            );
        }

        @Override
        public int successfulCharges() {
            return 0;
        }
    }

    private record ChargeThenRecordCoordinator(
            OrderWorkflowService workflow,
            PaymentAuthority paymentAuthority
    ) {
        private OrderActionResult pay(
                OrderId orderId,
                BigDecimal amount
        ) {
            paymentAuthority.charge(orderId, amount);
            return workflow.recordPayment(orderId);
        }
    }

    private record RecordThenChargeCoordinator(
            OrderWorkflowService workflow,
            PaymentAuthority paymentAuthority
    ) {
        private OrderActionResult pay(
                OrderId orderId,
                BigDecimal amount
        ) {
            var localResult = workflow.recordPayment(orderId);
            assertAccepted(localResult);
            paymentAuthority.charge(orderId, amount);
            return localResult;
        }
    }

    private static final class PaymentResponseLostException
            extends RuntimeException {
        private PaymentResponseLostException(String message) {
            super(message);
        }
    }

    private static final class PaymentProviderUnavailableException
            extends RuntimeException {
        private PaymentProviderUnavailableException(String message) {
            super(message);
        }
    }
}
