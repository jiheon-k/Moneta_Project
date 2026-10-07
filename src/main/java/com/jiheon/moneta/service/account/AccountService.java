package com.jiheon.moneta.service.account;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.jiheon.moneta.dto.account.AccountRequest;
import com.jiheon.moneta.entity.Account;
import com.jiheon.moneta.entity.User;
import com.jiheon.moneta.repository.AccountRepository;
import com.jiheon.moneta.repository.UserRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // 새로운 Account 생성
    public void createNewAccount(AccountRequest aRequest) {
        User user = userRepository.findById(aRequest.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 없습니다."));

        Account newAccount = new Account();
        newAccount.setUser(user);
        newAccount.setName(aRequest.getAccount_name());
        newAccount.setType(aRequest.getAccount_type());
        newAccount.setInitialBalance(aRequest.getInitialBalance());

        accountRepository.save(newAccount);
    }
}
