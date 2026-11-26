package com.rai.stockflow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Contrato estável para a listagem paginada de depósitos. */
public record WarehousePageResponse(
        List<WarehouseResponse> items,
        int page,
        int totalPages,
        long totalElements
) {
    public static WarehousePageResponse from(Page<WarehouseResponse> result) {
        return new WarehousePageResponse(result.getContent(), result.getNumber(),
                result.getTotalPages(), result.getTotalElements());
    }
}
