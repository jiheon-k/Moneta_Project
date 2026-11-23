package com.jiheon.moneta.config;

import com.jiheon.moneta.account.AccountRepository;
import com.jiheon.moneta.account.AccountService;
import com.jiheon.moneta.category.CategoryRepository;
import com.jiheon.moneta.transaction.TransactionRepository;
import com.jiheon.moneta.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

// demo 초기화도 별도 메모리 DB에서 확인하며 실제 실험용 파일은 사용하지 않는다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seed-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("demo")
class DemoDataInitializerTests {
    @Autowired private DemoDataInitializer initializer;
    @Autowired private UserRepository users;
    @Autowired private AccountRepository accounts;
    @Autowired private CategoryRepository categories;
    @Autowired private TransactionRepository transactions;
    @Autowired private AccountService service;

    @Test
    void createsExampleOnceAndKeepsItsBalance() {
        initializer.run();
        initializer.run();
        var user = users.findByLoginId("demo_user").orElseThrow();
        var account = accounts.findByUser_IdOrderByIdDesc(user.getId()).getFirst();
        assertEquals(1, users.count());
        assertEquals(1, accounts.count());
        assertEquals(2, categories.count());
        assertEquals(1, transactions.count());
        assertEquals(92000L, service.getAccount(account.getId(), user.getId()).getBalance());
    }
}
