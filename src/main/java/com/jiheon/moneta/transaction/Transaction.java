package com.jiheon.moneta.transaction;

import com.jiheon.moneta.user.User;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn(name="user_id", nullable=false)
    private User user;

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

    // 해당 거래에서 사용된 AccountType
    // JoinColum -> 참조할 칼럼 지정 X -> PK 참조
    // 돈이 나가는 계좌
    @ManyToOne
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    // 돈이 들어오는 계좌
    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    // NULl 허용
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public User getUser(){
        return user;
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

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
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

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
