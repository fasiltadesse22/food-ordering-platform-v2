package com.acme.foodordering.config;

import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.adapter.out.postgresql.PostgresOrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import com.acme.foodordering.application.port.in.GetOrderUseCase;
import com.acme.foodordering.application.port.in.ModifyOrderUseCase;
import com.acme.foodordering.application.port.in.OrderWorkflowUseCase;
import com.acme.foodordering.application.port.in.PlaceOrderUseCase;
import com.acme.foodordering.application.port.out.OrderRepository;
import com.acme.foodordering.application.service.GetOrderService;
import com.acme.foodordering.application.service.ModifyOrderService;
import com.acme.foodordering.application.service.OrderWorkflowService;
import com.acme.foodordering.application.service.PlaceOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @ConditionalOnProperty(name = "foodordering.repository.backend", havingValue = "memory", matchIfMissing = true)
    OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "foodordering.repository.backend", havingValue = "postgres")
    OrderRepository postgresOrderRepository(
            @Value("${foodordering.repository.postgres.jdbc-url}") String url,
            @Value("${foodordering.repository.postgres.username}") String username,
            @Value("${foodordering.repository.postgres.password}") String password
    ) {
        return new PostgresOrderRepository(url, username, password);
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

    @Bean
    ModifyOrderUseCase modifyOrderUseCase(OrderRepository orderRepository, Clock clock) {
        return new ModifyOrderService(orderRepository, clock);
    }
}
