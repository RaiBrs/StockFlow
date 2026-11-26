package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull(message = "Produto é obrigatório") @Positive(message = "Produto inválido") Long productId,
        @NotNull(message = "Depósito é obrigatório") @Positive(message = "Depósito inválido") Long warehouseId,
        @NotNull(message = "Quantidade é obrigatória") @Positive(message = "Quantidade deve ser positiva") Long quantity,
        @NotNull(message = "Direção é obrigatória") StockAdjustmentDirection direction,
        @NotBlank(message = "Justificativa é obrigatória")
        @Size(max = 500, message = "Justificativa deve ter no máximo 500 caracteres") String reason
) {
}
