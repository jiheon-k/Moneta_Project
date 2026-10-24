package com.jiheon.moneta.transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  // Income 총액
  // coalesce -> null 이면 0으로 설정
  // :accountId -> 함수 호출시 전달받을 인자
  @Query("""
      select coalesce(sum(t.amount), 0)
      from Transaction t
      where t.toAccount.id=:accountId
             """)
  Long sumIncomingAmount(@Param("accountId") Long accountId);

  // Expense 총액
  @Query("""
      select coalesce(sum(t.amount), 0)
      from Transaction t
      where t.fromAccount.id=:accountId
      """)
  Long sumOutgoingAmount(@Param("accountId") Long accountId);

  // 월별 수입및 지출 쿼리
  @Query("""
      select coalesce(sum(t.amount), 0)
      from Transaction t
      where t.fromAccount.id = :accountId
        and t.transactionDate >= :startDate
        and t.transactionDate < :endDate
      """)
  Long sumMonthlyOutgoingAmount(
      @Param("accountId") Long accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
      select coalesce(sum(t.amount), 0)
      from Transaction t
      where t.toAccount.id = :accountId
        and t.transactionDate >= :startDate
        and t.transactionDate < :endDate
      """)
  Long sumMonthlyIncomingAmount(
      @Param("accountId") Long accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  // 월별 수입 및 지출 리스트 쿼리
  @Query("""
      select t
      from Transaction t
      where t.fromAccount.id = :accountId
        and t.transactionDate >= :startDate
        and t.transactionDate < :endDate
      """)
  List<Transaction> periodOutgoingList(
      @Param("accountId") Long accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
      select t
      from Transaction t
      where t.toAccount.id = :accountId
        and t.transactionDate >= :startDate
        and t.transactionDate < :endDate
      """)
  List<Transaction> periodIncomingList(
      @Param("accountId") Long accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
      select t
      from Transaction t
      where (t.fromAccount.id = :accountId
          or t.toAccount.id = :accountId)
        and t.transactionDate >= :startDate
        and t.transactionDate < :endDate
      order by t.transactionDate desc, t.id desc
      """)
  List<Transaction> periodTransactionList(
      @Param("accountId") Long accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
