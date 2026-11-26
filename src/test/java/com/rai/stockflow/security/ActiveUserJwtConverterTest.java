package com.rai.stockflow.security;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveUserJwtConverterTest {

    private UserRepository repository;
    private ActiveUserJwtConverter converter;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        converter = new ActiveUserJwtConverter(repository);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("admin@example.com");
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMIN"));
    }

    @Test
    void grantsRoleFromTokenWhenItMatchesActiveUser() {
        when(repository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new User("admin@example.com", "hash", Role.ADMIN)));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertEquals("admin@example.com", authentication.getName());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void rejectsTokenAfterUserIsDeactivated() {
        User user = new User("admin@example.com", "hash", Role.ADMIN);
        user.deactivate();
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void rejectsTokenAfterRoleChanges() {
        when(repository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new User("admin@example.com", "hash", Role.MANAGER)));

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }
}
