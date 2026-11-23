package com.jiheon.moneta.auth.controller;

import com.jiheon.moneta.auth.dto.AuthUserResponse;
import com.jiheon.moneta.auth.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 현재 세션에 로그인한 사용자의 공개 정보를 반환한다.
@RestController
@RequestMapping("/auth")
public class CurrentUserController {
    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal LoginUser user) {
        return AuthUserResponse.from(user);
    }
}
