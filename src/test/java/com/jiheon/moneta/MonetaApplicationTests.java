package com.jiheon.moneta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 전체 앱 시작 검사는 테스트 전용 메모리 DB를 사용한다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:moneta-context-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class MonetaApplicationTests {

	@Test
	void contextLoads() {
	}

}
