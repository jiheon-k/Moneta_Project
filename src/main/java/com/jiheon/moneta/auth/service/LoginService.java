package com.jiheon.moneta.auth.service;

import java.nio.charset.StandardCharsets;

import com.jiheon.moneta.auth.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// 아이디·비밀번호 인증을 수행하고 인증 결과를 반환한다.
@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;

    public LoginService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public Authentication login(LoginRequest request) {
        // BCrypt의 입력 한계를 넘는 비밀번호도 500 대신 동일한 로그인 실패로 처리한다.
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw loginFailed();
        }
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getLoginId(), request.getPassword()));
        } catch (AuthenticationException ex) {
            // 계정 존재 여부와 비밀번호 오류를 구분해 노출하지 않는다.
            throw loginFailed();
        }
    }

    private ResponseStatusException loginFailed() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 올바르지 않습니다.");
    }
}
