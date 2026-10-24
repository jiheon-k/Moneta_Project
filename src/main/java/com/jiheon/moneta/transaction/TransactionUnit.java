package com.jiheon.moneta.transaction;

import java.time.LocalDate;

import com.jiheon.moneta.transaction.Transaction.TransactionType;

public class TransactionUnit {
    // Transcation에 있는 모든 정보 필요
    private Long userId;
    private Long id;
    private Long amount;
    private String memo;
    private LocalDate transactionDate;
    private TransactionType type;

    private Long fromAccountId;
    private Long toAccountId;
    private Long categoryId;
    
    public TransactionUnit(Long userId, Long id, Long amount, String memo, LocalDate transactionDate,
            TransactionType type, Long fromAccountId, Long toAccountId, Long categoryId) {
        this.userId=userId;
        this.id = id;
        this.amount = amount;
        this.memo = memo;
        this.transactionDate = transactionDate;
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.categoryId = categoryId;
    }

    public Long getUserId() {
        return userId;
    }

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

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

}
