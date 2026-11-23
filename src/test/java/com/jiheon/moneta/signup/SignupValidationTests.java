package com.jiheon.moneta.signup;

import com.jiheon.moneta.auth.controller.SignupController;
import com.jiheon.moneta.auth.dto.SignupRequest;
import com.jiheon.moneta.auth.service.SignupService;
import com.jiheon.moneta.common.GlobalExceptionHandler;
import com.jiheon.moneta.config.PasswordConfig;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignupValidationTests {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private SignupRequest validRequest() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("jiheon_1");
        request.setPassword("a long passphrase");
        request.setName("지헌");
        request.setAge(25);
        return request;
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("loginId", null),
                Arguments.of("loginId", ""),
                Arguments.of("loginId", "   "),
                Arguments.of("loginId", "abc"),
                Arguments.of("loginId", "a".repeat(21)),
                Arguments.of("loginId", "한글아이디"),
                Arguments.of("loginId", "user name"),
                Arguments.of("loginId", "user@name"),
                Arguments.of("password", null),
                Arguments.of("password", " ".repeat(15)),
                Arguments.of("password", "a".repeat(14)),
                Arguments.of("password", "a".repeat(65)),
                Arguments.of("password", "가".repeat(25)),
                Arguments.of("name", null),
                Arguments.of("name", ""),
                Arguments.of("name", "   "),
                Arguments.of("name", "가".repeat(51)),
                Arguments.of("name", "지헌123"),
                Arguments.of("name", "지헌!"),
                Arguments.of("name", " 지헌"),
                Arguments.of("name", "지헌 "),
                Arguments.of("age", null),
                Arguments.of("age", 0),
                Arguments.of("age", -1),
                Arguments.of("age", 151));
    }

    @ParameterizedTest(name = "invalid field {0}, case {index}")
    @MethodSource("invalidFields")
    void rejectsInvalidFields(String field, Object value) {
        SignupRequest request = validRequest();
        switch (field) {
            case "loginId" -> request.setLoginId((String) value);
            case "password" -> request.setPassword((String) value);
            case "name" -> request.setName((String) value);
            case "age" -> request.setAge((Integer) value);
            default -> throw new IllegalArgumentException(field);
        }
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsBoundariesAndInternationalNames() {
        SignupRequest request = validRequest();
        request.setLoginId("ab_1");
        request.setPassword("a".repeat(15));
        request.setName("김");
        request.setAge(1);
        assertTrue(validator.validate(request).isEmpty());

        request.setLoginId("A".repeat(20));
        request.setPassword("a".repeat(64));
        request.setName("가".repeat(50));
        request.setAge(150);
        assertTrue(validator.validate(request).isEmpty());

        for (String name : new String[] {"홍 길동", "Mary-Jane", "O'Connor", "张伟", "Иван", "A·B"}) {
            request.setName(name);
            assertTrue(validator.validate(request).isEmpty());
        }
    }

    @Test
    void acceptsMultibytePasswordAtBcryptByteBoundary() {
        SignupRequest request = validRequest();
        request.setPassword("가".repeat(24)); // UTF-8 72바이트
        assertTrue(validator.validate(request).isEmpty());
        var encoder = new PasswordConfig().passwordEncoder();
        String hash = encoder.encode(request.getPassword());
        assertTrue(encoder.matches(request.getPassword(), hash));
        assertFalse(encoder.matches("a different password", hash));
    }

    @Test
    void controllerRejectsInvalidInputBeforeCallingService() throws Exception {
        SignupService service = mock(SignupService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new SignupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        for (String body : new String[] {"{}", """
                {"loginId":"user_1","password":"short","name":"지헌","age":25}
                """, """
                {"loginId":"user_1","password":"%s","name":"지헌","age":25,
                 "passwordWithinByteLimit":true}
                """.formatted("가".repeat(25))}) {
            mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void controllerAcceptsValidRequest() throws Exception {
        SignupService service = mock(SignupService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new SignupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"loginId":"user_1","password":"a long passphrase","name":"지헌","age":25}
                """))
                .andExpect(status().isCreated());
        verify(service).signUp(argThat(request -> request.getLoginId().equals("user_1")));
    }
}
