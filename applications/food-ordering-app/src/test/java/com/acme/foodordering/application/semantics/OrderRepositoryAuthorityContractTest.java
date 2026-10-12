package com.acme.foodordering.application.semantics;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.port.out.OrderAlreadyExistsException;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.PlaceOrderService;
import com.acme.foodordering.domain.order.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class OrderRepositoryAuthorityContractTest {
    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final OrderId ID = OrderId.from("123e4567-e89b-12d3-a456-426614174110");
    private static final RestaurantId RESTAURANT = new RestaurantId("restaurant-repository");

    private static Order placed(CustomerId customer) {
        return Order.place(ID, customer, RESTAURANT,
                List.of(new OrderLine("burger-1","Burger",1,new BigDecimal("5.50"))),NOW);
    }

    @Test
    void createNewPublishesPreviouslyAbsentIdentityAndReturnsSameImmutableRepresentation() {
        var repo = new InMemoryOrderRepository();
        var order = placed(new CustomerId("customer-1"));
        assertThat(repo.findCurrentById(ID)).isEmpty();
        repo.createNew(order);
        assertThat(repo.findCurrentById(ID)).containsSame(order);
        assertThat(repo.size()).isEqualTo(1);
    }

    @Test
    void createNewRejectsSameIdAndDoesNotReplacePriorAuthority() {
        var repo = new InMemoryOrderRepository();
        var original = placed(new CustomerId("customer-1"));
        var collision = placed(new CustomerId("customer-2"));
        repo.createNew(original);
        assertThatThrownBy(() -> repo.createNew(collision))
                .isInstanceOf(OrderAlreadyExistsException.class)
                .satisfies(ex -> assertThat(((OrderAlreadyExistsException) ex).orderId()).isEqualTo(ID));
        assertThat(repo.findCurrentById(ID)).containsSame(original);
        assertThat(repo.findCurrentById(ID).orElseThrow().customerId())
                .isEqualTo(new CustomerId("customer-1"));
        assertThat(repo.size()).isEqualTo(1);
    }

    @Test
    void simultaneousCreatesForSameIdentityPublishExactlyOneRepresentation() throws Exception {
        var repo = new InMemoryOrderRepository();
        var candidate = placed(new CustomerId("customer-1"));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i=0;i<32;i++) {
                futures.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("start latch never released");
                    }
                    try {
                        repo.createNew(candidate);
                        return true;
                    } catch (OrderAlreadyExistsException expected) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int successfulCreations=0;
            for (var future : futures) {
                if (future.get(10, TimeUnit.SECONDS))successfulCreations++;
            }
            assertThat(successfulCreations).isEqualTo(1);
        }
        assertThat(repo.size()).isEqualTo(1);
        assertThat(repo.findCurrentById(ID)).containsSame(candidate);
    }

    @Test
    void existingSuccessorPublicationRemainsSeparateFromNewCreation() {
        var repo = new InMemoryOrderRepository();
        var initial = placed(new CustomerId("customer-1"));
        repo.createNew(initial);
        var accepted = initial.recordRestaurantAcceptance(RESTAURANT,NOW.plusSeconds(1));
        repo.saveCurrent(accepted);
        assertThat(repo.findCurrentById(ID)).containsSame(accepted);
        assertThat(accepted.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(initial.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void legacySaveCurrentStillAllowsStaleSuccessorToOverwriteNewerState() {
        var repo = new InMemoryOrderRepository();
        var initial = placed(new CustomerId("customer-1"));
        repo.createNew(initial);
        var accepted = initial.recordRestaurantAcceptance(RESTAURANT,NOW.plusSeconds(1));
        var cancelledFromStale = initial.recordCancellation(initial.customerId(),NOW.plusSeconds(2));
        repo.saveCurrent(accepted);
        repo.saveCurrent(cancelledFromStale);
        assertThat(repo.findCurrentById(ID).orElseThrow().status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(repo.findCurrentById(ID).orElseThrow().workflowOccurrences())
                .extracting(o->o.action().name())
                .containsExactly("ORDER_CANCELLED");
        // Negative control: P17 temporal failure intentionally preserved.
    }

    @Test
    void legacySaveCurrentCanStillInsertMissingIdentityAsDocumentedGap() {
        var repo = new InMemoryOrderRepository();
        repo.saveCurrent(placed(new CustomerId("customer-1")));
        assertThat(repo.findCurrentById(ID)).isPresent();
        // Negative control: update-only semantics/expected version remain deferred.
    }

    @Test
    void placeOrderServiceUsesCreateNewNotLegacySave() {
        var repository = new RecordingCreationRepository();
        var service = new PlaceOrderService(repository,CLOCK);
        var result = service.place(new PlaceOrderCommand("customer-1","restaurant-repository",
                List.of(new PlaceOrderCommand.Line("burger-1","Burger",1,new BigDecimal("5.50")))));
        assertThat(result).isInstanceOf(PlaceOrderResult.Accepted.class);
        assertThat(repository.creates).isEqualTo(1);
        assertThat(repository.legacySaves).isZero();
        var fact=((PlaceOrderResult.Accepted)result).fact();
        assertThat(repository.findCurrentById(fact.orderId())).isPresent();
    }

    @Test
    void identityConflictInCreationIsNotSilentlyReportedAsAcceptedPlacement() {
        var repository = new IdentityConflictRepository();
        var service = new PlaceOrderService(repository,CLOCK);
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "customer-1","restaurant-repository",
                List.of(new PlaceOrderCommand.Line("burger-1","Burger",1,new BigDecimal("5.50"))))))
                .isInstanceOf(OrderAlreadyExistsException.class);
        assertThat(repository.legacySaves).isZero();
    }

    @Test
    void newRepositoryInstanceHasNoPreviouslyCreatedOrder() {
        var first = new InMemoryOrderRepository();
        first.createNew(placed(new CustomerId("customer-1")));
        var independent = new InMemoryOrderRepository();
        assertThat(first.findCurrentById(ID)).isPresent();
        assertThat(independent.findCurrentById(ID)).isEmpty();
        // Process-local authority, NOT a surrogate for the P18 real SIGKILL experiment.
    }

    private static final class RecordingCreationRepository implements OrderRepository {
        private final InMemoryOrderRepository delegate = new InMemoryOrderRepository();
        private int creates;
        private int legacySaves;
        @Override public void createNew(Order order) {
            delegate.createNew(order);
            creates++;
        }
        @Override public void saveCurrent(Order order) {
            legacySaves++;
            delegate.saveCurrent(order);
        }
        @Override public Optional<Order> findCurrentById(OrderId id) {
            return delegate.findCurrentById(id);
        }
    }

    private static final class IdentityConflictRepository implements OrderRepository {
        private int legacySaves;
        @Override public void createNew(Order order) {
            throw new OrderAlreadyExistsException(order.id());
        }
        @Override public void saveCurrent(Order order) {
            legacySaves++;
            throw new AssertionError("create failure must not fall back to overwrite");
        }
        @Override public Optional<Order> findCurrentById(OrderId id) {
            return Optional.empty();
        }
    }
}
