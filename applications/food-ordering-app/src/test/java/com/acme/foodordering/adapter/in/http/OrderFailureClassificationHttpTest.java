package com.acme.foodordering.adapter.in.http;

import com.acme.foodordering.application.port.in.OrderWorkflowDecisionUseCase;
import com.acme.foodordering.domain.order.workflow.OrderWorkflowAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFailureClassificationHttpTest {
    @Autowired MockMvc mockMvc;

    private String place() throws Exception {
        var result = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"http-customer","restaurantId":"http-restaurant",
                     "lines":[{"menuItemId":"dish","name":"Dish","quantity":1,"unitPrice":13.00}]}
                    """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", not(blankOrNullString())))
                .andReturn();
        return result.getResponse().getHeader("Location").substring("/orders/".length());
    }

    @Test void restaurantRejectingPlacedOrderIsAnAcceptedBusinessDecisionHttp200() throws Exception {
        var id = place();
        mockMvc.perform(post("/orders/" + id + "/actions/RESTAURANT_REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test void invalidLifecycleCommandReturns409BusinessRejectionNot500() throws Exception {
        var id = place();
        mockMvc.perform(post("/orders/" + id + "/actions/ORDER_COMPLETED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.kind").value("BUSINESS_REJECTION"))
                .andExpect(jsonPath("$.code").value("ILLEGAL_LIFECYCLE_TRANSITION"))
                .andExpect(jsonPath("$.action").value("ORDER_COMPLETED"));
    }

    @Test void attemptedCompletionAfterRestaurantRejectionIsAlsoBusiness409() throws Exception {
        var id = place();
        mockMvc.perform(post("/orders/" + id + "/actions/RESTAURANT_REJECTED"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/orders/" + id + "/actions/ORDER_COMPLETED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.kind").value("BUSINESS_REJECTION"));
    }

    @Test void alreadyCancelledOrderCannotBeCancelledTwice() throws Exception {
        var id = place();
        mockMvc.perform(post("/orders/" + id + "/actions/ORDER_CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/orders/" + id + "/actions/ORDER_CANCELLED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_LIFECYCLE_TRANSITION"));
    }

    @Test void unknownOrderReturns404NotBusinessConflictOrTechnical500() throws Exception {
        mockMvc.perform(post("/orders/00000000-0000-0000-0000-000000000001/actions/ORDER_CANCELLED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"));
    }

    @Test void unknownActionNameIsBadInput400NotLifecycleConflict() throws Exception {
        var id = place();
        mockMvc.perform(post("/orders/" + id + "/actions/NOT_AN_ACTION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void unexpectedExceptionMapsToSafeHttp500WithoutLeakingInternalDetail() throws Exception {
        OrderWorkflowDecisionUseCase failing = (id, action) -> {
            throw new IllegalStateException("internal database connection SECRET");
        };
        var mvc = MockMvcBuilders.standaloneSetup(new OrderActionController(failing))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        mvc.perform(post("/orders/00000000-0000-0000-0000-000000000002/actions/ORDER_CANCELLED"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.kind").value("TECHNICAL_FAILURE"))
                .andExpect(jsonPath("$.detail").value("Internal request processing failed"))
                .andExpect(jsonPath("$.detail", not(containsString("SECRET"))));
    }
}
