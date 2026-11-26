package com.rai.stockflow.service;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.exception.DuplicateEmailException;
import com.rai.stockflow.exception.UserNotFoundException;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserRegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void normalizesEmailAndStoresEncodedPassword() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User saved = service.register("  ADMIN@Example.com  ", "secret", Role.ADMIN);

        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("encoded-secret", saved.getPasswordHash());
        assertEquals(Role.ADMIN, saved.getRole());
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void rejectsDuplicateEmailBeforeEncodingOrSaving() {
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new User("admin@example.com", "existing-hash", Role.ADMIN)));

        assertThrows(DuplicateEmailException.class,
                () -> service.register("ADMIN@example.com", "secret", Role.OPERATOR));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void missingUserThrowsSpecificException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.findById(99L));
    }
}
