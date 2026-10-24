package com.jiheon.moneta.account.dto;

import java.time.LocalDateTime;

import com.jiheon.moneta.account.Account.AccountType;

public class AccountResponse {
    private Long id;

    private AccountType type;

    private String name;

    private LocalDateTime createdAt;

    private Long initialBalance;

    private Long balance;

    public AccountResponse(Long id, AccountType type, String name, LocalDateTime createdAt, Long initialBalance, Long balance) {
        this.id=id;
        this.createdAt = createdAt;
        this.name = name;
        this.type = type;
        this.initialBalance=initialBalance;
        this.balance=balance;
    }

    public Long getId(){
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getInitialBalance(){
        return initialBalance;
    }

    public Long getBalance(){
        return balance;
    }

}
