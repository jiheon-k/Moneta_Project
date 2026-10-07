package com.jiheon.moneta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jiheon.moneta.entity.Account;
public interface AccountRepository extends JpaRepository<Account, Long>{

}
