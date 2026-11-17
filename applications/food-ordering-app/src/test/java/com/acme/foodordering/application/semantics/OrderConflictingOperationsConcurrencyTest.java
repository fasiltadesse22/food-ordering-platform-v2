package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderSnapshot;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * P13 Type C — reproduce genuine two-thread read/modify/write races with
 * deterministic read barriers and controlled writer order.
 *
 * Only this test code changes the execution schedule. The production
 * OrderWorkflowService and in-memory repository remain intentionally
 * vulnerable. A demonstration of a counterexample is not a concurrency fix.
 */
class OrderConflictingOperationsConcurrencyTest {
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    private Order placed() {
        return Order.place(OrderId.random(), new CustomerId("customer"),
                new RestaurantId("restaurant"),
                List.of(new OrderLine("dish", "Dish", 2, new BigDecimal("5.50"))), AT);
    }

    private static final class CoordinatedRepository implements OrderRepository {
        private final InMemoryOrderRepository actual = new InMemoryOrderRepository();
        private final CountDownLatch bothRead = new CountDownLatch(2);
        private final CountDownLatch releaseReads = new CountDownLatch(1);
        private final CountDownLatch firstSaved = new CountDownLatch(1);
        private final String firstWriter;
        private final List<String> trace = Collections.synchronizedList(new ArrayList<>());

        CoordinatedRepository(String firstWriter, Order... originals) {
            this.firstWriter = firstWriter;
            for (Order o : originals) actual.saveCurrent(o);
        }

        @Override public Optional<Order> findCurrentById(OrderId id) {
            // Capture actual reference BEFORE barrier: force equal-old-snapshot decisions.
            Optional<Order> snapshot = actual.findCurrentById(id);
            trace.add(who() + " READ " + snapshot.map(o -> o.status().toString()).orElse("MISSING")
                    + " history=" + snapshot.map(o -> o.workflowOccurrences().size()).orElse(-1));
            bothRead.countDown();
            await(releaseReads, "release both captured reads");
            return snapshot;
        }

        @Override public void saveCurrent(Order successor) {
            String actor = who();
            if (!actor.equals(firstWriter)) {
                await(firstSaved, "first writer's publication");
            }
            actual.saveCurrent(successor);  // the real production in-memory adapter
            trace.add(actor + " SAVE " + successor.status()
                    + " history=" + successor.workflowOccurrences().stream()
                        .map(o -> o.action().name()).toList());
            if (actor.equals(firstWriter)) firstSaved.countDown();
        }

        void release() { releaseReads.countDown(); }
        void unblockFailedTest() { releaseReads.countDown(); firstSaved.countDown(); }
        boolean waitForBothReads() throws InterruptedException {
            return bothRead.await(10, TimeUnit.SECONDS);
        }
        Order current(OrderId id) { return actual.findCurrentById(id).orElseThrow(); }
        List<String> trace() { return List.copyOf(trace); }

        private static String who() {
            String n = Thread.currentThread().getName();
            if (n.equals("p13-A")) return "A";
            if (n.equals("p13-B")) return "B";
            return n;
        }
        private static void await(CountDownLatch latch, String stage) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("P13 concurrency harness timed out at " + stage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted at " + stage, e);
            }
        }
    }

    private record ConcurrentRun(
            OrderSnapshot a, OrderSnapshot b,
            Order finalCurrent, List<String> trace
    ) {}

    private ConcurrentRun overlap(Order source, String firstWriter,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionA,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionB) throws Exception {
        return overlapTwo(new Order[]{source}, source.id(), firstWriter, actionA, actionB);
    }

    private ConcurrentRun overlapTwo(Order[] originals, Order readFinal, String firstWriter,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionA,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionB) throws Exception {
        return overlapTwo(originals, readFinal.id(), firstWriter, actionA, actionB);
    }

    private ConcurrentRun overlapTwo(Order[] originals, OrderId readFinal, String firstWriter,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionA,
            java.util.function.Function<OrderWorkflowService, OrderSnapshot> actionB) throws Exception {
        CoordinatedRepository repo = new CoordinatedRepository(firstWriter, originals);
        OrderWorkflowService sa = new OrderWorkflowService(repo, Clock.fixed(AT, ZoneOffset.UTC));
        OrderWorkflowService sb = new OrderWorkflowService(repo, Clock.fixed(AT, ZoneOffset.UTC));
        FutureTask<OrderSnapshot> fa = new FutureTask<>(() -> actionA.apply(sa));
        FutureTask<OrderSnapshot> fb = new FutureTask<>(() -> actionB.apply(sb));
        Thread ta = new Thread(fa, "p13-A");
        Thread tb = new Thread(fb, "p13-B");
        ta.start();
        tb.start();
        try {
            assertThat(repo.waitForBothReads())
                    .as("both real worker threads must capture source before either can decide")
                    .isTrue();
            repo.release();
            OrderSnapshot a = fa.get(10, TimeUnit.SECONDS);
            OrderSnapshot b = fb.get(10, TimeUnit.SECONDS);
            return new ConcurrentRun(a, b, repo.current(readFinal), repo.trace());
        } finally {
            repo.unblockFailedTest();
            ta.join(1000);
            tb.join(1000);
            if (ta.isAlive()) ta.interrupt();
            if (tb.isAlive()) tb.interrupt();
        }
    }

    private static boolean contains(Order order, OrderWorkflowAction action) {
        return order.workflowOccurrences().stream().anyMatch(e -> e.action() == action);
    }

    private static void assertBothRead(OrderId id, ConcurrentRun run, OrderStatus originalStatus) {
        assertThat(run.trace()).as("exact synchronized source references")
                .hasSize(4);
        assertThat(run.trace().stream().filter(s -> s.contains("READ " + originalStatus)).count())
                .as("both worker threads read the same initial Order state").isEqualTo(2L);
    }

    private static void assertWriteOrder(ConcurrentRun run, String first) {
        List<String> saves = run.trace().stream().filter(s -> s.contains(" SAVE ")).toList();
        assertThat(saves).hasSize(2);
        assertThat(saves.get(0)).startsWith(first + " SAVE ");
        assertThat(saves.get(1)).startsWith((first.equals("A") ? "B" : "A") + " SAVE ");
    }

    @Test void preparationThenCancellationLosesAnAcknowledgedPreparation() throws Exception {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        ConcurrentRun x = overlap(original, "A",
                s -> s.recordPreparationStarted(original.id()),
                s -> s.recordCancellation(original.id()));
        assertBothRead(original.id(), x, OrderStatus.ACCEPTED);
        assertWriteOrder(x, "A");
        assertThat(x.a().status()).isEqualTo("PREPARING");
        assertThat(x.b().status()).isEqualTo("CANCELLED");
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.ORDER_CANCELLED)).isTrue();
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.PREPARATION_STARTED)).isFalse();
        assertThat(x.a().workflow()).extracting(OrderSnapshot.WorkflowOccurrence::action)
                .contains(OrderWorkflowAction.PREPARATION_STARTED);
        // Failure: A told caller PREPARING, but current accepted history erased A.
    }

    @Test void cancellationThenPreparationLosesTerminalCancellation() throws Exception {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        ConcurrentRun x = overlap(original, "B",
                s -> s.recordPreparationStarted(original.id()),
                s -> s.recordCancellation(original.id()));
        assertBothRead(original.id(), x, OrderStatus.ACCEPTED);
        assertWriteOrder(x, "B");
        assertThat(x.b().status()).isEqualTo("CANCELLED");
        assertThat(x.a().status()).isEqualTo("PREPARING");
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.ORDER_CANCELLED)).isFalse();
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.PREPARATION_STARTED)).isTrue();
        // Stronger failure: a terminally cancelled result was acknowledged, then lost.
    }

    @Test void cancellationThenPaymentCanResurrectPlacedFromTerminalCancellation() throws Exception {
        Order original = placed();
        ConcurrentRun x = overlap(original, "A",
                s -> s.recordCancellation(original.id()),
                s -> s.recordPayment(original.id()));
        assertBothRead(original.id(), x, OrderStatus.PLACED);
        assertWriteOrder(x, "A");
        assertThat(x.a().status()).isEqualTo("CANCELLED");
        assertThat(x.b().status()).isEqualTo("PLACED");
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.PLACED);
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.ORDER_CANCELLED)).isFalse();
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.PAYMENT_RECORDED)).isTrue();
        // No provider capture; this is only a local PAYMENT_RECORDED milestone race.
    }

    @Test void paymentThenCancellationCanLoseAcceptedLocalPaymentMilestone() throws Exception {
        Order original = placed();
        ConcurrentRun x = overlap(original, "B",
                s -> s.recordCancellation(original.id()),
                s -> s.recordPayment(original.id()));
        assertBothRead(original.id(), x, OrderStatus.PLACED);
        assertWriteOrder(x, "B");
        assertThat(x.b().workflow()).extracting(OrderSnapshot.WorkflowOccurrence::action)
                .contains(OrderWorkflowAction.PAYMENT_RECORDED);
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.PAYMENT_RECORDED)).isFalse();
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.ORDER_CANCELLED)).isTrue();
    }

    @Test void twoOverlappingCancellationsCanBothReturnAcceptedAlthoughOneFactSurvives() throws Exception {
        Order original = placed();
        ConcurrentRun x = overlap(original, "A",
                s -> s.recordCancellation(original.id()),
                s -> s.recordCancellation(original.id()));
        assertThat(x.a().status()).isEqualTo("CANCELLED");
        assertThat(x.b().status()).isEqualTo("CANCELLED");
        assertThat(x.finalCurrent().workflowOccurrences()).hasSize(1);
        assertThat(x.finalCurrent().workflowOccurrences().get(0).action())
                .isEqualTo(OrderWorkflowAction.ORDER_CANCELLED);
        assertWriteOrder(x, "A");
        // Two accepted replies do not imply two retained facts; not an idempotency guarantee.
    }

    @Test void serialPreparationBeforeCancellationCorrectlyRejectsLateCancellation() {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        repo.saveCurrent(original);
        OrderWorkflowService service = new OrderWorkflowService(repo, Clock.fixed(AT, ZoneOffset.UTC));
        assertThat(service.recordPreparationStarted(original.id()).status()).isEqualTo("PREPARING");
        assertThatThrownBy(() -> service.recordCancellation(original.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);
        Order current = repo.findCurrentById(original.id()).orElseThrow();
        assertThat(current.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(contains(current, OrderWorkflowAction.PREPARATION_STARTED)).isTrue();
        assertThat(contains(current, OrderWorkflowAction.ORDER_CANCELLED)).isFalse();
    }

    @Test void serialCancellationBeforePreparationCorrectlyRejectsLatePreparation() {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        repo.saveCurrent(original);
        OrderWorkflowService service = new OrderWorkflowService(repo, Clock.fixed(AT, ZoneOffset.UTC));
        assertThat(service.recordCancellation(original.id()).status()).isEqualTo("CANCELLED");
        assertThatThrownBy(() -> service.recordPreparationStarted(original.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);
        Order current = repo.findCurrentById(original.id()).orElseThrow();
        assertThat(current.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(contains(current, OrderWorkflowAction.ORDER_CANCELLED)).isTrue();
        assertThat(contains(current, OrderWorkflowAction.PREPARATION_STARTED)).isFalse();
    }

    @Test void independentOrdersCanBeUpdatedByTheTwoThreadsWithoutLostFacts() throws Exception {
        Order first = placed();
        Order second = placed();
        ConcurrentRun x = overlapTwo(new Order[]{first, second}, first, "B",
                s -> s.recordCancellation(first.id()),
                s -> s.recordRestaurantAcceptance(second.id()));
        assertBothRead(first.id(), x, OrderStatus.PLACED);
        assertWriteOrder(x, "B");
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(contains(x.finalCurrent(), OrderWorkflowAction.ORDER_CANCELLED)).isTrue();
        assertThat(x.b().status()).isEqualTo("ACCEPTED");
        // The independent second Order is checked below via a separate test
        // fixture; the present schedule demonstrates no same-key conflict.
    }

    @Test void immutableSnapshotsCanBothBeLocallyValidEvenWhenGlobalHistoryLosesFact() throws Exception {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        ConcurrentRun x = overlap(original, "B",
                s -> s.recordPreparationStarted(original.id()),
                s -> s.recordCancellation(original.id()));
        assertThat(x.a().workflow()).extracting(OrderSnapshot.WorkflowOccurrence::action)
                .contains(OrderWorkflowAction.PREPARATION_STARTED);
        assertThat(x.b().workflow()).extracting(OrderSnapshot.WorkflowOccurrence::action)
                .contains(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(x.finalCurrent().workflowOccurrences()).extracting(e -> e.action())
                .doesNotContain(OrderWorkflowAction.ORDER_CANCELLED);
        assertThat(x.finalCurrent().status()).isEqualTo(OrderStatus.PREPARING);
        // P10 local invariant checks passed on BOTH values, so no local exception.
    }

    @Test void recordedTraceNamesBothReadsAndWriterCommitOrder() throws Exception {
        Order original = placed().recordPayment(AT).recordRestaurantAcceptance(AT);
        ConcurrentRun x = overlap(original, "A",
                s -> s.recordPreparationStarted(original.id()),
                s -> s.recordCancellation(original.id()));
        assertThat(x.trace()).filteredOn(t -> t.contains(" READ ")).hasSize(2);
        assertThat(x.trace()).filteredOn(t -> t.contains(" SAVE ")).hasSize(2);
        assertThat(x.trace()).anySatisfy(t -> assertThat(t).contains("A READ ACCEPTED"));
        assertThat(x.trace()).anySatisfy(t -> assertThat(t).contains("B READ ACCEPTED"));
        assertWriteOrder(x, "A");
    }
}
