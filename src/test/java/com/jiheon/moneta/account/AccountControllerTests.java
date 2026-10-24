package com.jiheon.moneta.account;

import com.jiheon.moneta.account.dto.AccountTransactionList;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.jiheon.moneta.transaction.Transaction.TransactionType;
import com.jiheon.moneta.transaction.TransactionUnit;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTests {
    private AccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService)).build();
    }

    @Test
    void allTransactionDatesAreConvertedAndDtoIsSerialized() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);
        TransactionUnit transaction = new TransactionUnit(2L, 3L, 10000L, "급여",
                start, TransactionType.INCOME, 1L, null, null);
        TransactionUnit expense = new TransactionUnit(2L, 4L, 3000L, "점심",
                start, TransactionType.EXPENSE, 1L, null, null);
        TransactionUnit transfer = new TransactionUnit(2L, 5L, 5000L, "저축",
                start, TransactionType.TRANSFER, 1L, 6L, null);
        when(accountService.getTransactions(1L, start, end, null))
                .thenReturn(new AccountTransactionList(1L, "생활비", List.of(transaction, expense, transfer)));

        mockMvc.perform(get("/accounts/1/transactions")
                        .param("start", "2026-10-01").param("end", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("생활비"))
                .andExpect(jsonPath("$.tList[0].userId").value(2))
                .andExpect(jsonPath("$.tList[0].amount").value(10000))
                .andExpect(jsonPath("$.tList.length()").value(3))
                .andExpect(jsonPath("$.tList[0].type").value("INCOME"))
                .andExpect(jsonPath("$.tList[0].accountId").value(1))
                .andExpect(jsonPath("$.tList[0].transferAccountId").isEmpty())
                .andExpect(jsonPath("$.tList[1].type").value("EXPENSE"))
                .andExpect(jsonPath("$.tList[2].type").value("TRANSFER"))
                .andExpect(jsonPath("$.tList[2].accountId").value(1))
                .andExpect(jsonPath("$.tList[2].transferAccountId").value(6));
        verify(accountService).getTransactions(1L, start, end, null);
    }

    @Test
    void transactionTypeIsPassedToService() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);
        mockMvc.perform(get("/accounts/1/transactions")
                        .param("start", "2026-10-01").param("end", "2026-11-01")
                        .param("type", "TRANSFER"))
                .andExpect(status().isOk());
        verify(accountService).getTransactions(1L, start, end, TransactionType.TRANSFER);
    }

    @Test
    void emptyTransactionListIsSerialized() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);
        when(accountService.getTransactions(1L, start, end, null))
                .thenReturn(new AccountTransactionList(1L, "생활비", List.of()));

        mockMvc.perform(get("/accounts/1/transactions")
                        .param("start", "2026-10-01").param("end", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tList").isEmpty());
        verify(accountService).getTransactions(1L, start, end, null);
    }

    @Test
    void omittedDatesUseOneMonthEndingTomorrow() throws Exception {
        LocalDate end = LocalDate.now().plusDays(1);
        mockMvc.perform(get("/accounts/1/transactions"))
                .andExpect(status().isOk());
        verify(accountService).getTransactions(1L, end.minusMonths(1), end, null);
    }

    @Test
    void invalidDatesAndPeriodsReturn400WithoutCallingService() throws Exception {
        String url = "/accounts/1/transactions";
        mockMvc.perform(get(url).param("start", "2026-02-30").param("end", "2026-11-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url).param("start", "2026/10/01").param("end", "2026-11-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url).param("start", "2026-11-01").param("end", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url).param("start", "2026-10-01").param("end", "2026-10-01"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test
    void missingAccountReturns404() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);
        when(accountService.getTransactions(999L, start, end, null))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/accounts/999/transactions")
                        .param("start", "2026-10-01").param("end", "2026-11-01"))
                .andExpect(status().isNotFound());
        verify(accountService).getTransactions(999L, start, end, null);
    }
}
