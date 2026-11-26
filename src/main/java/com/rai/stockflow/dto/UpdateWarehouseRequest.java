package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Atualização completa dos dados básicos do depósito. */
public record UpdateWarehouseRequest(
        @NotBlank(message = "Código é obrigatório") String code,
        @NotBlank(message = "Nome é obrigatório") String name
) {
}
