package com.acme.foodordering.application.semantics;

import com.acme.foodordering.application.port.in.OrderActionResult;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.domain.order.*;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowOccurrence;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class OrderAtomicityContractTest {
    private static final Instant NOW=Instant.parse("2026-10-11T11:00:00Z");
    private static final RestaurantId RESTAURANT=new RestaurantId("restaurant-1");
    private static final Clock CLOCK=Clock.fixed(NOW.plusSeconds(10),ZoneOffset.UTC);

    @Test void domainSuccessorHasNotCommittedUntilRepositoryAcceptsReplacement() {
        var initial=preparing();
        var current=new InProcessControlledRepository(initial);
        var successor=initial.recordCompletion(RESTAURANT,NOW.plusSeconds(10));
        assertThat(successor.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(current.findCurrentById(initial.id())).containsSame(initial);
        assertThat(current.findCurrentById(initial.id()).orElseThrow().status()).isEqualTo(OrderStatus.PREPARING);
        current.saveCurrent(successor);
        assertThat(current.findCurrentById(initial.id())).containsSame(successor);
    }

    @Test void repositoryFailureBeforePublicationLeavesPreviouslyCurrentRepresentation() {
        var initial=preparing();
        var repository=new InProcessControlledRepository(initial);
        repository.failBeforePublication=true;
        var service=new OrderWorkflowService(repository,CLOCK);
        assertThatThrownBy(()->service.recordCompletion(initial.id(),RESTAURANT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failure before publication");
        assertThat(repository.findCurrentById(initial.id())).containsSame(initial);
    }

    @Test void publicationThenFailureDoesNotUndoCurrentAuthority() {
        var initial=preparing();
        var repository=new InProcessControlledRepository(initial);
        repository.failAfterPublication=true;
        var service=new OrderWorkflowService(repository,CLOCK);
        assertThatThrownBy(()->service.recordCompletion(initial.id(),RESTAURANT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failure after publication");
        var published=repository.findCurrentById(initial.id()).orElseThrow();
        assertThat(published.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(published.workflowOccurrences()).extracting(OrderWorkflowOccurrence::action)
                .contains(OrderWorkflowAction.ORDER_COMPLETED);
        // An exception in the persistence adapter is not proof that no publication occurred.
        // In this controlled in-process scenario publication happened before the exception.
        // This is NOT a real DB commit/response-loss experiment.
    }

    @Test void failureDuringDomainDecisionDoesNotInvokeRepositorySave() {
        var initial=preparing();
        var repository=new InProcessControlledRepository(initial);
        var service=new OrderWorkflowService(repository,CLOCK);
        var result=service.recordRestaurantAcceptance(initial.id(),RESTAURANT);
        assertThat(result).isInstanceOf(OrderActionResult.Rejected.class);
        assertThat(repository.saves).isZero();
        assertThat(repository.findCurrentById(initial.id())).containsSame(initial);
    }

    private static Order preparing() {
        return Order.place(OrderId.from("123e4567-e89b-12d3-a456-426614174073"),
                new CustomerId("customer-1"),RESTAURANT,
                List.of(new OrderLine("burger-1","Burger",1,new BigDecimal("5.50"))), NOW)
                .recordRestaurantAcceptance(RESTAURANT,NOW.plusSeconds(1))
                .recordPreparationStarted(RESTAURANT,NOW.plusSeconds(2));
    }

    private static final class InProcessControlledRepository implements OrderRepository {
        private Order current;
        private int saves;
        private boolean failBeforePublication;
        private boolean failAfterPublication;
        private InProcessControlledRepository(Order current){this.current=current;}
        @Override public void saveCurrent(Order candidate){
            saves++;
            if(failBeforePublication) throw new IllegalStateException("failure before publication");
            current=candidate;
            if(failAfterPublication) throw new IllegalStateException("failure after publication");
        }
        @Override public Optional<Order> findCurrentById(OrderId id){
            return current.id().equals(id)?Optional.of(current):Optional.empty();
        }
    }
}
