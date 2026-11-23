package com.jiheon.moneta.auth;

import com.jiheon.moneta.auth.controller.CsrfController;
import com.jiheon.moneta.auth.controller.CurrentUserController;
import com.jiheon.moneta.auth.controller.LoginController;
import com.jiheon.moneta.auth.controller.SignupController;
import com.jiheon.moneta.auth.dto.CsrfResponse;
import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.auth.security.LoginUserDetailsService;
import com.jiheon.moneta.auth.security.SecurityErrorHandler;
import com.jiheon.moneta.auth.service.LoginService;
import com.jiheon.moneta.auth.service.SignupService;
import com.jiheon.moneta.common.GlobalExceptionHandler;
import com.jiheon.moneta.config.PasswordConfig;
import com.jiheon.moneta.config.SecurityConfig;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 테스트 전용 H2 메모리 DB와 인증 구성만 사용한다. 개발 MySQL 및 다른 기능은 로드하지 않는다.
@SpringBootTest(classes = AuthIntegrationTests.TestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class AuthIntegrationTests {
    private static final String PASSWORD = "a long test passphrase";
    private final JsonMapper mapper = new JsonMapper();
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void clearTestDatabase() {
        users.deleteAll();
    }

    private record Client(MockHttpSession session, String header, String token) {}

    private Client csrf(MockHttpSession session) throws Exception {
        var builder = get("/auth/csrf");
        if (session != null) builder.session(session);
        var result = mvc.perform(builder).andExpect(status().isOk()).andReturn();
        CsrfResponse token = mapper.readValue(result.getResponse().getContentAsString(), CsrfResponse.class);
        return new Client((MockHttpSession) result.getRequest().getSession(false), token.headerName(), token.token());
    }

    private ResultActions send(Client client, String path, Map<String, ?> body) throws Exception {
        return mvc.perform(post(path).session(client.session()).header(client.header(), client.token())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private void signup(Client client) throws Exception {
        send(client, "/auth/signup", Map.of("loginId", "user_1", "password", PASSWORD,
                "name", "지헌", "age", 25)).andExpect(status().isCreated());
    }

    private ResultActions login(Client client, String loginId, String password) throws Exception {
        return send(client, "/auth/login", Map.of("loginId", loginId, "password", password));
    }

    @Test
    void signupStoresHashAndDoesNotAutomaticallyLogin() throws Exception {
        Client client = csrf(null);
        signup(client);
        User user = users.findByLoginId("user_1").orElseThrow();
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(encoder.matches(PASSWORD, user.getPasswordHash()));
        assertEquals("지헌", user.getName());
        mvc.perform(get("/auth/me").session(client.session()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void duplicateSignupReturns409AndKeepsOneUser() throws Exception {
        Client client = csrf(null);
        signup(client);
        send(client, "/auth/signup", Map.of("loginId", "user_1", "password", PASSWORD,
                "name", "지헌", "age", 25))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertEquals(1, users.count());
    }

    @Test
    void loginPersistsAuthenticationRotatesSessionAndErasesCredentials() throws Exception {
        Client client = csrf(null);
        signup(client);
        String oldId = client.session().getId();
        login(client, "user_1", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("user_1"))
                .andExpect(jsonPath("$.id").value(users.findByLoginId("user_1").orElseThrow().getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertNotEquals(oldId, client.session().getId());
        SecurityContext context = (SecurityContext) client.session().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(context);
        assertNull(context.getAuthentication().getCredentials());
        assertNull(((LoginUser) context.getAuthentication().getPrincipal()).getPassword());
        mvc.perform(get("/auth/me").session(client.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("user_1"));
    }

    @Test
    void wrongPasswordAndUnknownUserHaveSame401Response() throws Exception {
        Client client = csrf(null);
        signup(client);
        var wrong = login(client, "user_1", "wrong passphrase")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        var missing = login(client, "missing_user", PASSWORD)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(wrong, missing);
        assertNull(client.session().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void loginRejectsOversizedBcryptInputWithoutServerError() throws Exception {
        Client client = csrf(null);
        signup(client);
        login(client, "user_1", "가".repeat(25))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void csrfIsRequiredAndRefreshedAfterLoginThenLogoutInvalidatesSession() throws Exception {
        Client beforeLogin = csrf(null);
        signup(beforeLogin);
        login(beforeLogin, "user_1", PASSWORD).andExpect(status().isOk());
        send(beforeLogin, "/auth/logout", Map.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Client afterLogin = csrf(beforeLogin.session());
        send(afterLogin, "/auth/logout", Map.of())
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(afterLogin.session().isInvalid());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        // 로그아웃 뒤에는 새 세션과 새 CSRF 토큰으로 다시 로그인할 수 있다.
        login(csrf(null), "user_1", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void missingCsrfAndInvalidSignupNeverPersistUser() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        send(csrf(null), "/auth/signup", Map.of("loginId", "bad", "password", "short", "name", "", "age", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        send(csrf(null), "/auth/login", Map.of())
                .andExpect(status().isBadRequest());
        assertEquals(0, users.count());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({SecurityConfig.class, PasswordConfig.class, SecurityErrorHandler.class,
            LoginUserDetailsService.class, SignupService.class, LoginService.class,
            SignupController.class, LoginController.class, CurrentUserController.class,
            CsrfController.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }
}
