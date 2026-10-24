package com.jiheon.moneta.account.dto;

import java.util.List;
import com.jiheon.moneta.transaction.TransactionUnit;

public class AccountTransactionList {
    private Long id;
    private String name;
    private List<TransactionUnit> tList;

    public AccountTransactionList(Long id, String name, List<TransactionUnit> list) {
        this.id = id;
        this.name = name;
        this.tList = list;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TransactionUnit> getTList() {
        return tList;
    }

}
