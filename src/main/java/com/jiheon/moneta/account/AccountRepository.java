package com.jiheon.moneta.account;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// 계좌를 DB에 저장하고 사용자별 계좌를 조회한다.
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser_IdOrderByIdDesc(Long userId);
    Optional<Account> findByIdAndUser_Id(Long accountId, Long userId);
}
