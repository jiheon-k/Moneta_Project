package com.jiheon.moneta.account;

import java.time.LocalDate;
import java.util.List;
import com.jiheon.moneta.account.dto.AccountSummary;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.jiheon.moneta.account.dto.AccountRequest;
import com.jiheon.moneta.account.dto.AccountResponse;
import com.jiheon.moneta.account.dto.AccountTransactionList;
import com.jiheon.moneta.account.dto.AccountUpdateRequest;
import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.transaction.Transaction.TransactionType;

import jakarta.validation.Valid;

// 계좌 요청과 조회 조건을 받아 로그인 사용자 기준으로 Service를 호출한다.
@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountSummary> getAccounts(@AuthenticationPrincipal LoginUser loginUser) {
        return accountService.getAccounts(loginUser.getId());
    }

    // 계좌 생성
    @PostMapping()
    public Long createAccount(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody AccountRequest request) {
        return accountService.createNewAccount(loginUser.getId(), request);
    }

    // 계좌 삭제
    // 해당 계좌 관련 이체 내역 존재 -> 삭제 실패
    // 이후 논의
    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id,
            @AuthenticationPrincipal LoginUser user) {
        accountService.deleteAccount(id, user.getId());
    }

    // 계좌 수정
    @PatchMapping("/{id}")
    public void changeAccount(
            @PathVariable("id") Long accountId,
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody AccountUpdateRequest request) {
        accountService.updateAccount(accountId, loginUser.getId(), request);
    }

    // 계좌 단건 조회
    @GetMapping("{id}")
    public AccountResponse getAccountById(
            @PathVariable("id") Long accountId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return accountService.getAccount(accountId, loginUser.getId());
    }

    // 시작일 포함, 종료일 제외. 생략하면 내일을 종료일로 최근 한 달 조회
    @GetMapping("/{id}/transactions")
    public AccountTransactionList getAllTransaction(
            @PathVariable("id") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) TransactionType type,
            @AuthenticationPrincipal LoginUser loginUser) {
        LocalDate endDate = (end == null) ? LocalDate.now().plusDays(1) : end;
        LocalDate startDate = (start == null) ? endDate.minusMonths(1) : start;
        validatePeriod(startDate, endDate);
        return accountService.getTransactions(accountId, loginUser.getId(), startDate, endDate, type);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다.");
        }
    }
}
