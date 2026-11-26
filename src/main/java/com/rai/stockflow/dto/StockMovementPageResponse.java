package com.rai.stockflow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Evita expor diretamente o formato interno de Page na API. */
public record StockMovementPageResponse(
        List<StockMovementResponse> items,
        int page,
        int totalPages,
        long totalElements
) {
    public static StockMovementPageResponse from(Page<StockMovementResponse> result) {
        return new StockMovementPageResponse(result.getContent(), result.getNumber(),
                result.getTotalPages(), result.getTotalElements());
    }
}
