package com.acme.foodordering.adapter.in.http;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @Test
    void unexpectedRuntimeFailureMapsTo500WithoutPretendingItIsBadRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/technical-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Unexpected technical failure"))
                .andExpect(jsonPath("$.detail")
                        .value("The server could not complete the request."));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/technical-failure")
        String fail() {
            throw new NullPointerException("programming bug must not become 400");
        }
    }
}
