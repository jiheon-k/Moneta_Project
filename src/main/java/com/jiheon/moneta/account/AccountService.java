package com.jiheon.moneta.account;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.jiheon.moneta.transaction.TransactionUnit;
import com.jiheon.moneta.account.dto.*;
import com.jiheon.moneta.transaction.Transaction;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.transaction.TransactionRepository;
import com.jiheon.moneta.user.UserRepository;

import jakarta.transaction.Transactional;

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

        // =========== 계좌 생성 / 삭제 / 수정 ===========
        // 새로운 Account 1개 생성
        public Long createNewAccount(AccountRequest aRequest) {
                User user = userRepository.findById(aRequest.getUserId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 없습니다."));

                Account newAccount = new Account();
                newAccount.setUser(user);
                newAccount.setName(aRequest.getAccount_name());
                newAccount.setType(aRequest.getAccount_type());
                newAccount.setInitialBalance(aRequest.getInitialBalance());

                accountRepository.save(newAccount);
                return newAccount.getId();
        }

        // 계좌 1개 삭제
        // 해당 계좌 관련 이체 내역 존재시 실패 처리
        public void deleteAccount(Long id) {
                Account findAccount = accountRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "해당 계좌가 존재하지 않습니다."));

                accountRepository.delete(findAccount);
        }

        // 계좌 정보 
        @Transactional
        public void updateAccount(Long id, AccountUpdateRequest urequest) {
                Account getAccount = accountRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "해당 계좌가 존재하지 않습니다."));
                if(urequest.getAccount_name()!=null){
                        getAccount.setName(urequest.getAccount_name());
                }

                if(urequest.getAccount_type()!=null){
                        getAccount.setType(urequest.getAccount_type());
                }

                if(urequest.getInitialBalance()!=null){
                        getAccount.setInitialBalance(urequest.getInitialBalance());
                }
        }

        // =========== 계좌 관련 정보 ===========
        // 기본 정보 - id, type, name, createAt, 잔액, 초기 잔액
        public AccountResponse getAccount(Long id) {
                Account getAccount = accountRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "해당 계좌가 존재하지 않습니다."));

                Long balance = transactionRepository.sumIncomingAmount(id)
                                - transactionRepository.sumOutgoingAmount(id) + getAccount.getInitialBalance();
                AccountResponse accountResponse = new AccountResponse(getAccount.getId(), getAccount.getType(),
                                getAccount.getName(),
                                getAccount.getCreatedAt(), getAccount.getInitialBalance(), balance);

                return accountResponse;
        }

        // =========== 거래 내역 조회 -> 기간을 정해서 조회 가능 ===========
        // default -> 최근 한달
        // 1. 계좌 모든 거래 내역
        public AccountTransactionList getAllTransactionList(Long id, LocalDate start, LocalDate end) {
                Account account = accountRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "해당 계좌가 존재하지 않습니다."));

                List<Transaction> list = transactionRepository.periodTransactionList(id, start, end);

                List<TransactionUnit> result = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                        Transaction t = list.get(i);
                        Long fromId = t.getFromAccount() == null ? null : t.getFromAccount().getId();
                        Long toId = t.getToAccount() == null ? null : t.getToAccount().getId();
                        Long categoryId = t.getCategory() == null ? null : t.getCategory().getId();

                        TransactionUnit tu = new TransactionUnit(t.getUser().getId(), t.getId(), t.getAmount(),
                                        t.getMemo(), t.getTransactionDate(), t.getType(), fromId, toId, categoryId);
                        result.add(tu);
                }
                return new AccountTransactionList(id, account.getName(), result);
        }

}
