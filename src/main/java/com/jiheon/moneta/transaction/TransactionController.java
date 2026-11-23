package com.jiheon.moneta.transaction;

import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.transaction.dto.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 지출 등록 요청을 검증하고 로그인 사용자 정보와 함께 Service에 전달한다.
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
}
