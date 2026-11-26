package com.rai.stockflow.service;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.exception.DuplicateEmailException;
import com.rai.stockflow.exception.UserNotFoundException;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Cadastra usuários com e-mail normalizado e senha codificada. */
@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        // A senha original não deve chegar à entidade persistida.
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(normalizedEmail, passwordHash, role);
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
