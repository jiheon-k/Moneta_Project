package com.jiheon.moneta.config;

import com.jiheon.moneta.account.Account;
import com.jiheon.moneta.account.AccountRepository;
import com.jiheon.moneta.category.Category;
import com.jiheon.moneta.category.CategoryRepository;
import com.jiheon.moneta.transaction.Transaction;
import com.jiheon.moneta.transaction.TransactionRepository;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// demo 모드의 첫 실행에만 예제 데이터를 만든다. 재실행해도 사용자가 입력한 기록은 유지한다.
@Component
@Profile("demo")
public class DemoDataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final PasswordEncoder encoder;

    public DemoDataInitializer(UserRepository users, AccountRepository accounts,
            CategoryRepository categories, TransactionRepository transactions, PasswordEncoder encoder) {
        this.users = users;
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByLoginId("demo_user").isPresent()) return;

        User user = new User();
        user.setLoginId("demo_user");
        // 공개된 실험용 계정이다. 실제 사용자 비밀번호가 아니다.
        user.setPasswordHash(encoder.encode("moneta-demo-1234"));
        user.setName("체험 사용자");
        user.setAge(25);
        users.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setName("체험용 생활비 통장");
        account.setType(Account.AccountType.LIVING);
        account.setInitialBalance(100000L);
        accounts.save(account);

        Category food = new Category();
        food.setName("식비");
        categories.save(food);

        Category hobby = new Category();
        hobby.setName("취미");
        hobby.setUser(user);
        categories.save(hobby);

        transactions.save(Transaction.expense(account, food, 8000L, LocalDate.now(), "예제: 점심 식사"));
    }
}
