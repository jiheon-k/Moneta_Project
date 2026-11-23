package com.jiheon.moneta.auth.controller;

import com.jiheon.moneta.auth.dto.CsrfResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 변경 요청 검증에 사용할 현재 세션의 CSRF 토큰을 반환한다.
@RestController
@RequestMapping("/auth")
public class CsrfController {
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }
}
