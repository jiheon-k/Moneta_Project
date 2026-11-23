package com.jiheon.moneta.auth;

import com.jiheon.moneta.auth.dto.SignupRequest;
import com.jiheon.moneta.auth.service.SignupService;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignupServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SignupService service = new SignupService(users, encoder);

    private SignupRequest request() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("user_1");
        request.setPassword("a long test passphrase");
        request.setName("지헌");
        request.setAge(25);
        return request;
    }

    @Test
    void concurrentDuplicateDetectedByDatabaseReturns409() {
        when(users.existsByLoginId("user_1")).thenReturn(false, true);
        when(encoder.encode(anyString())).thenReturn("test-hash");
        when(users.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        var exception = assertThrows(ResponseStatusException.class, () -> service.signUp(request()));

        assertEquals(409, exception.getStatusCode().value());
        verify(users, times(2)).existsByLoginId("user_1");
    }

    @Test
    void unrelatedDatabaseConstraintIsNotReportedAsDuplicateLoginId() {
        when(users.existsByLoginId("user_1")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("test-hash");
        var failure = new DataIntegrityViolationException("other constraint");
        when(users.saveAndFlush(any(User.class))).thenThrow(failure);

        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.signUp(request())));
    }
}
