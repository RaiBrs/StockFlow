package com.rai.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados aceitos no futuro cadastro de produtos pela API. */
public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotBlank String category
) {
}
