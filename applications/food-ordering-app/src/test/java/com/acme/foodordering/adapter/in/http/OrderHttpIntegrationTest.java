package com.acme.foodordering.adapter.in.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
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
        var mvcResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
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
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(11.0))
                .andReturn();

        var location = mvcResult.getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.restaurantId").value("restaurant-1"))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void rejectsAnEmptyOrderAsBadRequest() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "restaurantId": "restaurant-1",
                                  "lines": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
