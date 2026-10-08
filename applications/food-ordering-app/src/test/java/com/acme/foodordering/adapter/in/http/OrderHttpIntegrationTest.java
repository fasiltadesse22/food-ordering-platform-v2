package com.acme.foodordering.adapter.in.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderHttpIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void placesAndReadsAnOrderThroughHttp() throws Exception {
        var location = placeOrder("customer-1");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.restaurantId").value("restaurant-1"))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void emptyOrderLinesFailHttpBoundaryValidation() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-empty",
                                  "restaurantId": "restaurant-1",
                                  "lines": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void blankCustomerIdFailsHttpBoundaryValidation() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": " ",
                                  "restaurantId": "restaurant-1",
                                  "lines": [
                                    {
                                      "menuItemId": "burger-1",
                                      "name": "Classic Burger",
                                      "quantity": 1,
                                      "unitPrice": 5.50
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void nonPositiveQuantityFailsHttpBoundaryValidation() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-validation",
                                  "restaurantId": "restaurant-1",
                                  "lines": [
                                    {
                                      "menuItemId": "burger-1",
                                      "name": "Classic Burger",
                                      "quantity": 0,
                                      "unitPrice": 5.50
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void validCancellationReturnsSuccessButRepeatedCancellationIsBusinessConflict() throws Exception {
        var location = placeOrder("customer-cancel");
        var orderId = location.substring("/orders/".length());

        mockMvc.perform(post("/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"customer-cancel"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"customer-cancel"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
    }

    @Test
    void blankCancellationCustomerFailsValidationBeforeBusinessEvaluation() throws Exception {
        var location = placeOrder("customer-cancel-validation");
        var orderId = location.substring("/orders/".length());

        mockMvc.perform(post("/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void wrongCustomerCancellationIsForbiddenBusinessRejection() throws Exception {
        var location = placeOrder("customer-owner");
        var orderId = location.substring("/orders/".length());

        mockMvc.perform(post("/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"customer-other"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("CUSTOMER_DOES_NOT_OWN_ORDER"));
    }

    @Test
    void unknownOrderCancellationIsNotFoundBusinessRejection() throws Exception {
        mockMvc.perform(post(
                        "/orders/123e4567-e89b-12d3-a456-426614174099/cancel"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"customer-1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private String placeOrder(String customerId) throws Exception {
        var mvcResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "restaurantId": "restaurant-1",
                                  "lines": [
                                    {
                                      "menuItemId": "burger-1",
                                      "name": "Classic Burger",
                                      "quantity": 2,
                                      "unitPrice": 5.50
                                    }
                                  ]
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        not(blankOrNullString())
                ))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(11.0))
                .andReturn();

        return mvcResult.getResponse().getHeader("Location");
    }
}
