package com.jiheon.moneta.account;

import com.jiheon.moneta.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountErrorResponseTests {
    private AccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void missingAccountPreservesReason() throws Exception {
        when(accountService.getAccount(999L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 계좌가 존재하지 않습니다."));

        mockMvc.perform(get("/accounts/view/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 계좌가 존재하지 않습니다."));
    }

    @Test
    void invalidRequestFieldsReturn400() throws Exception {
        mockMvc.perform(post("/accounts/create").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("initialBalance")));
        verifyNoInteractions(accountService);
    }

    @Test
    void malformedJsonAndInvalidEnumReturn400() throws Exception {
        for (String body : new String[] {"{", "{\"account_type\":\"INVALID\"}"}) {
            mockMvc.perform(post("/accounts/create").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(accountService);
    }

    @Test
    void invalidPathAndDateReturn400() throws Exception {
        mockMvc.perform(get("/accounts/view/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(get("/accounts/view/1/transactions/incoming").param("start", "2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(accountService);
    }

    @Test
    void invalidPeriodPreservesReason() throws Exception {
        mockMvc.perform(get("/accounts/view/1/transactions/incoming")
                        .param("start", "2026-11-01").param("end", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("시작일은 종료일보다 이전이어야 합니다."));
        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"UNAUTHORIZED", "FORBIDDEN", "CONFLICT"})
    void explicitStatusesUseCommonFormat(HttpStatus httpStatus) throws Exception {
        when(accountService.getAccount(1L)).thenThrow(new ResponseStatusException(httpStatus));
        mockMvc.perform(get("/accounts/view/1"))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.code").value(httpStatus.name()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalMessage() throws Exception {
        when(accountService.getAccount(1L)).thenThrow(new IllegalStateException("private database details"));
        mockMvc.perform(get("/accounts/view/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }
}
