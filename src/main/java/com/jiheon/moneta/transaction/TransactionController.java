package com.jiheon.moneta.transaction;

import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.transaction.dto.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 지출 요청을 검증하고 로그인 사용자 정보와 함께 Service에 전달한다.
@RestController
@RequestMapping("/expenses")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createExpense(@AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody TransactionRequest request) {
        return service.createExpense(user.getId(), request);
    }

    @GetMapping("/{id}")
    public TransactionUnit getExpense(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return service.getExpense(id, user.getId());
    }

    @PutMapping("/{id}")
    public TransactionUnit updateExpense(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody TransactionRequest request) {
        return service.updateExpense(id, user.getId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        service.deleteExpense(id, user.getId());
    }
}
