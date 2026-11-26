package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockChangeRequest(
        @NotNull(message = "Produto é obrigatório") @Positive(message = "Produto inválido") Long productId,
        @NotNull(message = "Depósito é obrigatório") @Positive(message = "Depósito inválido") Long warehouseId,
        @NotNull(message = "Quantidade é obrigatória") @Positive(message = "Quantidade deve ser positiva") Long quantity
) {
}
