package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados aceitos no futuro cadastro de depósitos pela API. */
public record CreateWarehouseRequest(
        @NotBlank(message = "Código é obrigatório") String code,
        @NotBlank(message = "Nome é obrigatório") String name
) {
}
