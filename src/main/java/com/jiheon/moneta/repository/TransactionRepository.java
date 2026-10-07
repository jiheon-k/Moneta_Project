package com.jiheon.moneta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiheon.moneta.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
