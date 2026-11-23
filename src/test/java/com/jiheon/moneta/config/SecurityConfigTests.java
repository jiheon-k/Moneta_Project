package com.jiheon.moneta.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.jiheon.moneta.auth.security.SecurityErrorHandler;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTests {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void protectedGetRequiresAuthentication() throws Exception {
        mvc.perform(get("/private")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicPostStillRequiresCsrfToken() throws Exception {
        for (String path : new String[] {"/auth/signup", "/auth/login"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden());
        }
    }

    @Test
    void publicPostWithCsrfTokenDoesNotRequireLogin() throws Exception {
        var request = mvc.perform(get("/private")).andReturn().getRequest();
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String tokenValue = token.getToken();
        MockHttpSession session = (MockHttpSession) request.getSession();

        for (String path : new String[] {"/auth/signup", "/auth/login"}) {
            mvc.perform(post(path).session(session).header(token.getHeaderName(), tokenValue))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/private").session(session).header(token.getHeaderName(), tokenValue))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, PasswordConfig.class, SecurityErrorHandler.class, TestController.class})
    static class TestConfig {
        @Bean
        UserDetailsService userDetailsService() {
            return loginId -> { throw new UsernameNotFoundException("test"); };
        }
    }

    // 실제 가입·로그인을 수행하지 않고 보안 필터의 접근 규칙만 검증한다.
    @RestController
    static class TestController {
        @GetMapping("/private")
        public void privateGet() {
        }

        @PostMapping({"/auth/signup", "/auth/login", "/private"})
        public void postEndpoint() {
        }
    }
}
