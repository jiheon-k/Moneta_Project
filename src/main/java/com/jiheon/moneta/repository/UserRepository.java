package com.jiheon.moneta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiheon.moneta.entity.User;
public interface UserRepository extends JpaRepository<User, Long>{

    // JPA의 Query Method는 메서드 이름을 분석하여 Entity 필드를 찾음
    // Repository에서는 선언만
    List<User> findByAgeGreaterThanEqual(int age);
}
