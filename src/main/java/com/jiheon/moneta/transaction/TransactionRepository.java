package com.jiheon.moneta.transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiheon.moneta.transaction.Transaction.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 계좌로 들어온 금액 합산
    // 수입 + 해당 계좌로 이체한 돈 
    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where ((t.account.id = :accountId and t.type = INCOME)
                or (t.transferAccount.id = :accountId and t.type = TRANSFER))
                   """)
    Long sumIncomingAmount(@Param("accountId") Long accountId);

    // 계좌에서 나간 금액 합산(이체 포함)
    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.account.id = :accountId
              and t.type in (EXPENSE,
                             TRANSFER)
            """)
    Long sumOutgoingAmount(@Param("accountId") Long accountId);

    // 기간별 유출·유입 합계 (이체 포함)
    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.account.id = :accountId
              and t.type in (EXPENSE,
                             TRANSFER)
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
            where ((t.account.id = :accountId and t.type = INCOME)
                or (t.transferAccount.id = :accountId and t.type = TRANSFER))
              and t.transactionDate >= :startDate
              and t.transactionDate < :endDate
            """)
    Long sumMonthlyIncomingAmount(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 기간별 유출·유입 목록 (이체 포함)
    @Query("""
            select t
            from Transaction t
            where t.account.id = :accountId
              and t.type in (EXPENSE,
                             TRANSFER)
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
            where ((t.account.id = :accountId and t.type = INCOME)
                or (t.transferAccount.id = :accountId and t.type = TRANSFER))
              and t.transactionDate >= :startDate
              and t.transactionDate < :endDate
            """)
    List<Transaction> periodIncomingList(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 계좌 관련 전체 거래 목록
    // 타입 필터링 가능
    @Query("""
            select t
            from Transaction t
            where (t.account.id = :accountId
                or (t.transferAccount.id = :accountId
                    and t.type = TRANSFER))
              and (:type is null or t.type = :type)
              and t.transactionDate >= :startDate
              and t.transactionDate < :endDate
            order by t.transactionDate desc, t.id desc
            """)
    List<Transaction> periodTypeTransactionList(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type);

}
