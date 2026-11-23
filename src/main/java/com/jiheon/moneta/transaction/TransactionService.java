package com.jiheon.moneta.transaction;

import com.jiheon.moneta.account.Account;
import com.jiheon.moneta.account.AccountRepository;
import com.jiheon.moneta.category.Category;
import com.jiheon.moneta.category.CategoryRepository;
import com.jiheon.moneta.transaction.dto.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 지출 계좌·카테고리의 사용 권한을 확인하고 하나의 작업으로 저장한다.
@Service
public class TransactionService {
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;

    public TransactionService(AccountRepository accounts, CategoryRepository categories,
            TransactionRepository transactions) {
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
    }

    @Transactional
    public Long createExpense(Long userId, TransactionRequest request) {
        Account account = accounts.findByIdAndUser_Id(request.accountId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 계좌가 존재하지 않습니다."));
        Category category = null;
        if (request.categoryId() != null) {
            category = categories.findAvailable(request.categoryId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용할 수 있는 카테고리가 없습니다."));
        }
        Transaction expense = Transaction.expense(account, category, request.amount(),
                request.transactionDate(), request.memo());
        return transactions.save(expense).getId();
    }
}
