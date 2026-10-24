package com.jiheon.moneta.account.dto;

import com.jiheon.moneta.common.GlobalExceptionHandler;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountUpdateRequestTests {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DtoTestController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void acceptsPartialRequest() throws Exception {
        mockMvc.perform(patch("/dto-test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_name\":\"생활비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account_name").value("생활비"));
    }

    @Test
    void acceptsTypeAndZeroBalance() throws Exception {
        mockMvc.perform(patch("/dto-test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_type\":\"SAVING\",\"initialBalance\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account_type").value("SAVING"))
                .andExpect(jsonPath("$.initialBalance").value(0));
    }

    @Test
    void rejectsExplicitNullForEachField() throws Exception {
        for (String field : new String[] {"account_name", "account_type", "initialBalance"}) {
            mockMvc.perform(patch("/dto-test").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"" + field + "\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @Test
    void rejectsBlankAndLongNames() throws Exception {
        for (String name : new String[] {"", "   ", "a".repeat(101)}) {
            mockMvc.perform(patch("/dto-test").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"account_name\":\"" + name + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @RestController
    static class DtoTestController {
        @PatchMapping("/dto-test")
        public AccountUpdateRequest update(@Valid @RequestBody AccountUpdateRequest request) {
            return request;
        }
    }
}
