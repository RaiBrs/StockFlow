package com.rai.stockflow.dto;

import com.rai.stockflow.entity.Warehouse;

/** Define os dados do depósito que poderão ser enviados pela API. */
public record WarehouseResponse(
        Long id,
        String code,
        String name,
        boolean active
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.isActive()
        );
    }
}
