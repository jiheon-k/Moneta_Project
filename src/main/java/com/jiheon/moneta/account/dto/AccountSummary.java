package com.jiheon.moneta.account.dto;

import com.jiheon.moneta.account.Account.AccountType;

// 계좌 선택에 필요한 정보만 반환한다. 잔액은 선택한 계좌의 상세 조회에서 계산한다.
public record AccountSummary(Long id, String name, AccountType type) {
}
