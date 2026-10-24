package com.jiheon.moneta.account.dto;

import com.jiheon.moneta.account.Account.AccountType;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountUpdateRequest {
    // 길이 제한 + 이름에 공백이 아닌 문자가 있는지 검사
    // NotBlank -> NULL, 빈 문자열, 공백 문자열 모두 거부
    // 따라서 name에 NULL 혀용, 이상한 문자열 거부
    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "계좌 이름은 공백일 수 없습니다.")
    private String account_name;

    private AccountType account_type;

    private Long initialBalance;

    public String getAccount_name() {
        return account_name;
    }

    // JSON에 해당 필드가 있고, 그 값이 null이면 변환 실패 
    @JsonSetter(nulls = Nulls.FAIL)
    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public AccountType getAccount_type() {
        return account_type;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setAccount_type(AccountType account_type) {
        this.account_type = account_type;
    }

    public Long getInitialBalance() {
        return initialBalance;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setInitialBalance(Long initialBalance) {
        this.initialBalance = initialBalance;
    }
}
