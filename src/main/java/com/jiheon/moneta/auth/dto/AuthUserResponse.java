package com.jiheon.moneta.auth.dto;

import com.jiheon.moneta.auth.security.LoginUser;

public record AuthUserResponse(Long id, String loginId) {
    public static AuthUserResponse from(LoginUser user) {
        return new AuthUserResponse(user.getId(), user.getUsername());
    }
}
