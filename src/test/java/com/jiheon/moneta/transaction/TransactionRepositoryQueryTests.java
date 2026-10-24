package com.jiheon.moneta.transaction;

import com.jiheon.moneta.account.Account;
import com.jiheon.moneta.category.Category;
import com.jiheon.moneta.user.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

// DB 접속 없이 엔티티 매핑과 Repository의 HQL 필드·타입을 검사한다.
class TransactionRepositoryQueryTests {
    @Test
    void repositoryQueriesMatchRenamedEntityFields() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Transaction.class)
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(Category.class)
                .addAnnotatedClass(User.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none");

        try (SessionFactory factory = configuration.buildSessionFactory();
             var session = factory.openSession()) {
            for (var method : TransactionRepository.class.getDeclaredMethods()) {
                Query query = method.getAnnotation(Query.class);
                if (query != null) {
                    Class<?> resultType = method.getReturnType() == Long.class ? Long.class : Transaction.class;
                    session.createQuery(query.value(), resultType);
                }
            }
        }
    }
}
