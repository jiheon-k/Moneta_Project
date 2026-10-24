package com.jiheon.moneta.account;

import com.jiheon.moneta.account.dto.AccountRequest;
import com.jiheon.moneta.account.dto.AccountResponse;
import com.jiheon.moneta.account.dto.AccountTransactionList;
import com.jiheon.moneta.account.dto.AccountUpdateRequest;

import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // 계좌 1개 생성 = 완료
    @PostMapping("/create")
    public Long createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        // @Valid -> DTO 검증
        Long accountId = accountService.createNewAccount(accountRequest);
        return accountId;
    }

    // 계좌 삭제 
    // 해당 계좌 관련 이체 내역 존재 -> 삭제 실패
    // 이후 논의
    @DeleteMapping("/delete/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }

    @PatchMapping("/change/{id}")
    public void changeAccount(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest accountUpdate){
        accountService.updateAccount(id, accountUpdate);
    }

    // 계좌 1개 간단 정보
    @GetMapping("/view/{id}")
    public AccountResponse getAccountById(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    // 시작일 포함, 종료일 제외. 생략하면 내일을 종료일로 최근 한 달 조회
    @GetMapping("/view/{id}/transactions/incoming")
    public AccountTransactionList getAllTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate endDate = (end == null) ? LocalDate.now().plusDays(1) : end;
        LocalDate startDate = (start == null) ? endDate.minusMonths(1) : start;
        validatePeriod(startDate, endDate);
        return accountService.getAllTransactionList(id, startDate, endDate);
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다.");
        }
    }

}
