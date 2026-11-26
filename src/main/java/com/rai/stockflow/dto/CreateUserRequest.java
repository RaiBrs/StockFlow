package com.rai.stockflow.dto;

import com.rai.stockflow.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Contrato de cadastro; a senha nunca aparece na resposta. */
public record CreateUserRequest(
        @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "Senha é obrigatória") String password,
        @NotNull(message = "Perfil é obrigatório") Role role
) {
}
