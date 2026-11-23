package com.jiheon.moneta.auth.security;

import com.jiheon.moneta.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security가 인증할 사용자를 로그인 ID로 조회한다.
@Service
public class LoginUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public LoginUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        return userRepository.findByLoginId(loginId)
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
