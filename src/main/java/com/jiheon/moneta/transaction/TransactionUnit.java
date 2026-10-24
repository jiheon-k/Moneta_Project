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

    private Long accountId;
    private Long transferAccountId;
    private Long categoryId;
    
    public TransactionUnit(Long userId, Long id, Long amount, String memo, LocalDate transactionDate,
            TransactionType type, Long accountId, Long transferAccountId, Long categoryId) {
        this.userId=userId;
        this.id = id;
        this.amount = amount;
        this.memo = memo;
        this.transactionDate = transactionDate;
        this.type = type;
        this.accountId = accountId;
        this.transferAccountId = transferAccountId;
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

    public Long getAccountId() {
        return accountId;
    }

    public Long getTransferAccountId() {
        return transferAccountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

}
