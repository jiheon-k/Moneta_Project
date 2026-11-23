package com.jiheon.moneta.auth.security;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiheon.moneta.user.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 인증에 필요한 정보만 보관한다. JPA Entity 자체를 세션에 저장하지 않는다.
public class LoginUser implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String loginId;
    private String passwordHash;

    public LoginUser(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.passwordHash = user.getPasswordHash();
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    // 인증 완료 후 ProviderManager가 호출하여 세션에 해시가 남지 않게 한다.
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
