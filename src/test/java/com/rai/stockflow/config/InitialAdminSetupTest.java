package com.rai.stockflow.config;

import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.security.Role;
import com.rai.stockflow.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitialAdminSetupTest {

    private UserRepository userRepository;
    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        registrationService = mock(UserRegistrationService.class);
    }

    @Test
    void createsAdminOnlyWhenNoUsersExist() {
        InitialAdminSetup setup = new InitialAdminSetup(
                userRepository, registrationService, "admin@example.com", "secret");

        setup.run(null);

        verify(registrationService).register("admin@example.com", "secret", Role.ADMIN);
    }

    @Test
    void rejectsMissingCredentials() {
        InitialAdminSetup setup = new InitialAdminSetup(userRepository, registrationService, "", "secret");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> setup.run(null));

        assertEquals("Informe o e-mail e a senha do administrador inicial.", error.getMessage());
        verifyNoInteractions(registrationService);
    }

    @Test
    void rejectsSetupWhenAUserAlreadyExists() {
        when(userRepository.count()).thenReturn(1L);
        InitialAdminSetup setup = new InitialAdminSetup(
                userRepository, registrationService, "admin@example.com", "secret");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> setup.run(null));

        assertEquals("A configuração inicial exige uma tabela de usuários vazia.", error.getMessage());
        verifyNoInteractions(registrationService);
    }
}
