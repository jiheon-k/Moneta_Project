package com.jiheon.moneta.transaction;

import com.jiheon.moneta.account.Account;
import com.jiheon.moneta.account.AccountRepository;
import com.jiheon.moneta.category.Category;
import com.jiheon.moneta.category.CategoryRepository;
import com.jiheon.moneta.transaction.Transaction.TransactionType;
import com.jiheon.moneta.transaction.dto.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 지출과 연결 항목의 사용 권한을 확인하고 조회·저장·삭제를 처리한다.
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
        Account account = getOwnedAccount(request.accountId(), userId);
        Category category = getAvailableCategory(request.categoryId(), userId);
        Transaction expense = Transaction.expense(account, category, request.amount(),
                request.transactionDate(), request.memo());
        return transactions.save(expense).getId();
    }

    @Transactional(readOnly = true)
    public TransactionUnit getExpense(Long id, Long userId) {
        Transaction expense = transactions.findByIdAndAccount_User_IdAndType(id, userId, TransactionType.EXPENSE)
                .orElseThrow(TransactionService::expenseNotFound);
        return TransactionUnit.from(expense);
    }

    @Transactional
    public TransactionUnit updateExpense(Long id, Long userId, TransactionRequest request) {
        Transaction expense = getExpenseForUpdate(id, userId);
        Account account = getOwnedAccount(request.accountId(), userId);
        Category category = getAvailableCategory(request.categoryId(), userId);
        expense.updateExpense(account, category, request.amount(), request.transactionDate(), request.memo());
        return TransactionUnit.from(expense);
    }

    @Transactional
    public void deleteExpense(Long id, Long userId) {
        transactions.delete(getExpenseForUpdate(id, userId));
    }

    private Transaction getExpenseForUpdate(Long id, Long userId) {
        return transactions.findForUpdateByIdAndAccount_User_IdAndType(id, userId, TransactionType.EXPENSE)
                .orElseThrow(TransactionService::expenseNotFound);
    }

    private static ResponseStatusException expenseNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 지출이 존재하지 않습니다.");
    }

    private Account getOwnedAccount(Long accountId, Long userId) {
        return accounts.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 계좌가 존재하지 않습니다."));
    }

    private Category getAvailableCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;
        }
        return categories.findAvailable(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용할 수 있는 카테고리가 없습니다."));
    }
}
