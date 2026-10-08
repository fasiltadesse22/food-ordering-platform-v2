package com.acme.foodordering.config;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.GetOrderService;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PlaceOrderUseCase placeOrderUseCase(OrderRepository orderRepository, Clock clock) {
        return new PlaceOrderService(orderRepository, clock);
    }

    @Bean
    GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderService(orderRepository);
    }

    @Bean
    OrderWorkflowUseCase orderWorkflowUseCase(OrderRepository orderRepository, Clock clock) {
        return new OrderWorkflowService(orderRepository, clock);
    }
}
