package com.rai.stockflow.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Ativa a verificação de perfis declarada nos métodos dos controllers. */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
