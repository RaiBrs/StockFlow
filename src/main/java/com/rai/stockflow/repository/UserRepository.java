package com.rai.stockflow.repository;

import com.rai.stockflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Consulta de usuários usada no cadastro e, futuramente, na autenticação. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
