package com.acme.foodordering.adapter.in.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P10 dedicated adapter validation tests. Not business invariant checks. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderValidationBoundaryTest {
    @Autowired MockMvc mvc;

    @Test void blankCustomerIdIsRejectedAtInputBoundary() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"customerId":" ","restaurantId":"r","lines":[
             {"menuItemId":"dish","name":"Dish","quantity":1,"unitPrice":12.00}]}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void zeroQuantityIsRejectedAsStructuralInput() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"customerId":"c","restaurantId":"r","lines":[
             {"menuItemId":"dish","name":"Dish","quantity":0,"unitPrice":12.00}]}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void negativeUnitPriceIsRejectedAsStructuralInput() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"customerId":"c","restaurantId":"r","lines":[
             {"menuItemId":"dish","name":"Dish","quantity":1,"unitPrice":-1}]}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void missingLinesIsRejectedAtInputBoundary() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"customerId":"c","restaurantId":"r"}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void malformedOrderIdIsInvalidInputNotInvariantFailure() throws Exception {
        mvc.perform(post("/orders/not-a-uuid/actions/ORDER_COMPLETED"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
    }

    @Test void syntacticallyValidRequestPassesValidationButViolatesLifecycleBusinessRule() throws Exception {
        var created = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"customerId":"c","restaurantId":"r","lines":[
             {"menuItemId":"dish","name":"Dish","quantity":1,"unitPrice":12.00}]}
            """)).andExpect(status().isCreated())
            .andExpect(header().string("Location", not(blankOrNullString())))
            .andReturn();
        var uri = created.getResponse().getHeader("Location");
        mvc.perform(post(uri + "/actions/ORDER_COMPLETED"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.kind").value("BUSINESS_REJECTION"))
            .andExpect(jsonPath("$.code").value("ILLEGAL_LIFECYCLE_TRANSITION"));
    }
}
