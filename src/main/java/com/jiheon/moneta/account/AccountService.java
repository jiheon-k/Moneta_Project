package com.jiheon.moneta.account;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.jiheon.moneta.account.dto.AccountRequest;
import com.jiheon.moneta.account.dto.AccountSummary;
import org.springframework.dao.DataIntegrityViolationException;
import com.jiheon.moneta.account.dto.AccountResponse;
import com.jiheon.moneta.account.dto.AccountTransactionList;
import com.jiheon.moneta.account.dto.AccountUpdateRequest;
import com.jiheon.moneta.transaction.Transaction;
import com.jiheon.moneta.transaction.Transaction.TransactionType;
import com.jiheon.moneta.transaction.TransactionRepository;
import com.jiheon.moneta.transaction.TransactionUnit;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;

import org.springframework.transaction.annotation.Transactional;

// 계좌 소유권과 변경 규칙을 확인하고 거래 합계로 잔액을 계산한다.
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountSummary> getAccounts(Long userId) {
        return accountRepository.findByUser_IdOrderByIdDesc(userId).stream()
                .map(account -> new AccountSummary(account.getId(), account.getName(), account.getType()))
                .toList();
    }

    // 계좌 생성
    public Long createNewAccount(Long userId, AccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 없습니다."));

        Account account = new Account();
        account.setUser(user);
        account.setName(request.getAccount_name());
        account.setType(request.getAccount_type());
        account.setInitialBalance(request.getInitialBalance());

        accountRepository.save(account);
        return account.getId();
    }

    // 거래 기록을 보존하기 위해 거래가 연결된 계좌는 삭제하지 않는다.
    @Transactional
    public void deleteAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 계좌가 존재하지 않습니다."));
        if (transactionRepository.existsByAccount_IdOrTransferAccount_Id(id, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "거래가 있는 계좌는 삭제할 수 없습니다.");
        }
        try {
            accountRepository.delete(account);
            accountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 기록에서 사용 중인 계좌는 삭제할 수 없습니다.");
        }
    }

    // 계좌 수정
    @Transactional
    public void updateAccount(Long accountId, Long userId, AccountUpdateRequest request) {
        Account account = accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "해당 계좌가 존재하지 않습니다."));

        if (request.getAccount_name() != null) {
            account.setName(request.getAccount_name());
        }

        if (request.getAccount_type() != null) {
            account.setType(request.getAccount_type());
        }

        if (request.getInitialBalance() != null) {
            if (!request.getInitialBalance().equals(account.getInitialBalance())
                    && transactionRepository.existsByAccount_IdOrTransferAccount_Id(accountId, accountId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "거래가 있는 계좌의 초기 잔액은 변경할 수 없습니다.");
            }
            account.setInitialBalance(request.getInitialBalance());
        }
    }

    // 계좌 단건 조회 및 잔액 계산
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "해당 계좌가 존재하지 않습니다."));

        Long balance = transactionRepository.sumIncomingAmount(accountId)
                - transactionRepository.sumOutgoingAmount(accountId) + account.getInitialBalance();
        AccountResponse response = new AccountResponse(
                account.getId(), account.getType(), account.getName(),
                account.getCreatedAt(), account.getInitialBalance(), balance);

        return response;
    }

    // 계좌의 기간별 거래 내역 조회
    @Transactional(readOnly = true)
    public AccountTransactionList getTransactions(
            Long accountId, Long userId, LocalDate startDate, LocalDate endDate, TransactionType type) {
        Account account = accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "해당 계좌가 존재하지 않습니다."));

        List<Transaction> transactions = transactionRepository.periodTypeTransactionList(
                accountId, startDate, endDate, type);
        List<TransactionUnit> transactionUnits = new ArrayList<>();

        for (int index = 0; index < transactions.size(); index++) {
            Transaction transaction = transactions.get(index);
            Long transactionAccountId = transaction.getAccount().getId();
            Long transferAccountId = transaction.getTransferAccount() == null
                    ? null : transaction.getTransferAccount().getId();
            Long categoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId();

            TransactionUnit transactionUnit = new TransactionUnit(
                    transaction.getAccount().getUser().getId(), transaction.getId(), transaction.getAmount(),
                    transaction.getMemo(), transaction.getTransactionDate(), transaction.getType(),
                    transactionAccountId, transferAccountId, categoryId);
            transactionUnits.add(transactionUnit);
        }

        return new AccountTransactionList(accountId, account.getName(), transactionUnits);
    }
}
