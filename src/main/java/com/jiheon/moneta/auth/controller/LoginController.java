package com.jiheon.moneta.auth.controller;

import com.jiheon.moneta.auth.dto.AuthUserResponse;
import com.jiheon.moneta.auth.dto.LoginRequest;
import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.auth.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인 요청을 검증하고 인증된 사용자의 세션을 설정한다.
@RestController
@RequestMapping("/auth")
public class LoginController {
    private final LoginService loginService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;

    public LoginController(LoginService loginService,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository) {
        this.loginService = loginService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request, HttpServletResponse response) {
        var authentication = loginService.login(loginRequest);

        // 세션 ID 교체 및 기존 CSRF 토큰 제거 후 인증 결과를 세션에 저장한다.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return AuthUserResponse.from((LoginUser) authentication.getPrincipal());
    }
}
