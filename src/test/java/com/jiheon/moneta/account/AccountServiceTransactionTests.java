package com.jiheon.moneta.account;

import com.jiheon.moneta.transaction.Transaction;
import com.jiheon.moneta.transaction.Transaction.TransactionType;
import com.jiheon.moneta.transaction.TransactionRepository;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTransactionTests {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final AccountService service = new AccountService(accounts, mock(UserRepository.class), transactions);

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void mapsAccountAndOptionalTransferAccount(TransactionType type) {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = start.plusMonths(1);
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(1L);
        when(account.getName()).thenReturn("생활비");
        when(accounts.findById(1L)).thenReturn(Optional.of(account));

        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        Transaction transaction = mock(Transaction.class);
        when(transaction.getAccount()).thenReturn(account);
        when(transaction.getUser()).thenReturn(user);
        when(transaction.getType()).thenReturn(type);
        if (type == TransactionType.TRANSFER) {
            Account receivingAccount = mock(Account.class);
            when(receivingAccount.getId()).thenReturn(6L);
            when(transaction.getTransferAccount()).thenReturn(receivingAccount);
        }
        when(transactions.periodTypeTransactionList(1L, start, end, type)).thenReturn(List.of(transaction));

        var result = service.getTransactions(1L, start, end, type).getTList().getFirst();

        assertEquals(1L, result.getAccountId());
        assertEquals(type == TransactionType.TRANSFER ? 6L : null, result.getTransferAccountId());
        assertNull(result.getCategoryId());
        assertEquals(type, result.getType());
        verify(transactions).periodTypeTransactionList(1L, start, end, type);
    }

    @Test
    void missingAccountReturns404BeforeQueryingTransactions() {
        when(accounts.findById(999L)).thenReturn(Optional.empty());
        LocalDate start = LocalDate.of(2026, 10, 1);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.getTransactions(999L, start, start.plusMonths(1), null));

        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(transactions);
    }
}
