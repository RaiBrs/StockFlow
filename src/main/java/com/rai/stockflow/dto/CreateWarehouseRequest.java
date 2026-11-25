package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados aceitos no futuro cadastro de depósitos pela API. */
public record CreateWarehouseRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
