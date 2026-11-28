package com.jiheon.moneta.transaction;

import com.jiheon.moneta.account.Account;
import com.jiheon.moneta.category.Category;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Transaction {
    // 지출은 기준 계좌만 가지며, 이체 도착 계좌는 설정하지 않는다.
    public static Transaction expense(Account account, Category category, Long amount,
            LocalDate date, String memo) {
        Transaction transaction = new Transaction();
        transaction.type = TransactionType.EXPENSE;
        transaction.updateExpense(account, category, amount, date, memo);
        return transaction;
    }

    void updateExpense(Account account, Category category, Long amount, LocalDate date, String memo) {
        if (type != TransactionType.EXPENSE) {
            throw new IllegalStateException("지출 기록만 수정할 수 있습니다.");
        }
        if (account == null || amount == null || amount <= 0 || amount > 1_000_000_000_000L
                || date == null || date.isAfter(LocalDate.now()) || (memo != null && memo.length() > 255)) {
            throw new IllegalArgumentException("지출 계좌, 금액, 날짜와 메모를 확인해 주세요.");
        }
        this.account = account;
        this.category = category;
        this.amount = amount;
        this.transactionDate = date;
        this.memo = memo == null ? null : memo.trim();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long amount;

    private String memo;
    @Column(nullable = false)
    private LocalDate transactionDate;

    public enum TransactionType {
        INCOME,
        EXPENSE,
        TRANSFER
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // 수입·지출을 기록할 계좌. 이체에서는 출금 계좌
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 이체받는 계좌. 수입·지출에서는 null
    @ManyToOne
    @JoinColumn(name = "transfer_account_id")
    private Account transferAccount;

    // NULl 허용
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Long getId() {
        return id;
    }

    public Long getAmount() {
        return amount;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public TransactionType getType() {
        return type;
    }

    public Account getAccount() {
        return account;
    }

    public Account getTransferAccount() {
        return transferAccount;
    }

    public Category getCategory() {
        return category;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setTransferAccount(Account transferAccount) {
        this.transferAccount = transferAccount;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
