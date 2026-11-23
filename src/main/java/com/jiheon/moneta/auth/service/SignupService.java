package com.jiheon.moneta.auth.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.jiheon.moneta.auth.dto.SignupRequest;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;

// 로그인 ID 중복을 확인하고 비밀번호를 암호화해 사용자를 저장한다.
@Service
public class SignupService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public SignupService(
            UserRepository userRepo,
            PasswordEncoder passwordEncoder) {

        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입시 로그인 ID 중복 확인
    public void checkLoginId(SignupRequest signupRequest) {
        if (userRepo.existsByLoginId(signupRequest.getLoginId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
    }

    // 회원가입 처리
    public void signUp(SignupRequest signupRequest) {
        checkLoginId(signupRequest);
        // 비밀번호 hash값으로 변경
        String passwordHash = passwordEncoder.encode(signupRequest.getPassword());

        User user = new User();
        user.setAge(signupRequest.getAge());
        user.setLoginId(signupRequest.getLoginId());
        user.setPasswordHash(passwordHash);
        user.setName(signupRequest.getName());

        try {
            // DB의 UNIQUE 제약으로 동시에 들어온 중복 가입도 막는다.
            // Repository 트랜잭션에서 오류가 확정된 뒤 중복 여부를 확인한다.
            userRepo.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            checkLoginId(signupRequest);
            throw ex;
        }
    }
}
