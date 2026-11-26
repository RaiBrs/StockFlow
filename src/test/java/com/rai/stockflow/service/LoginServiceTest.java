package com.rai.stockflow.service;

import com.rai.stockflow.dto.LoginRequest;
import com.rai.stockflow.dto.LoginResponse;
import com.rai.stockflow.entity.User;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private UserRepository repository;
    private JwtEncoder jwtEncoder;
    private LoginService service;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        jwtEncoder = mock(JwtEncoder.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new LoginService(repository, passwordEncoder, jwtEncoder);
    }

    @Test
    void validPasswordIssuesShortBearerToken() {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-token");
        when(jwtEncoder.encode(any())).thenReturn(jwt);

        LoginResponse response = service.login(new LoginRequest(" ADMIN@example.com ", "correct"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
    }

    @Test
    void wrongPasswordDoesNotIssueToken() {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("admin@example.com", "wrong")));

        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void inactiveUserDoesNotIssueToken() {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        user.deactivate();
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("admin@example.com", "correct")));

        verifyNoInteractions(jwtEncoder);
    }
}
