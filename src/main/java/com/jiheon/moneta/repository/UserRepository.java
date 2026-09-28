package com.jiheon.moneta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiheon.moneta.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
}
