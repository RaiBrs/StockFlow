package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados aceitos no futuro cadastro de produtos pela API. */
public record CreateProductRequest(
        @NotBlank(message = "SKU é obrigatório") String sku,
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotBlank(message = "Categoria é obrigatória") String category
) {
}
