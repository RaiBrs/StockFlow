package com.rai.stockflow.security;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/** Confere o estado e o perfil atuais antes de aceitar as roles do JWT. */
@Component
public class ActiveUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public ActiveUserJwtConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = userRepository.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Token inválido."));
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (!user.isActive() || roles == null || roles.size() != 1
                || !roles.getFirst().equals(user.getRole().name())) {
            throw new BadCredentialsException("Token inválido.");
        }

        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + roles.getFirst())), user.getEmail());
    }
}
