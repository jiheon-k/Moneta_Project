package com.jiheon.moneta.user;

import org.springframework.data.jpa.repository.JpaRepository;
// User를 DB에 저장하고 조회할 Repository 생성, 기본 기능은 JpaRepository에서 물려받음
public interface UserRepository extends JpaRepository<User, Long>{

}
