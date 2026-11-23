package com.jiheon.moneta.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
// User를 DB에 저장하고 조회할 Repository 생성, 기본 기능은 JpaRepository에서 물려받음
// 사용자를 저장하고 ID나 로그인 ID로 DB에서 조회한다.
public interface UserRepository extends JpaRepository<User, Long>{

    // 로그인 ID 중복 여부 확인xw
    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

}
