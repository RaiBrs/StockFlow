package com.rai.stockflow.dto;

import com.rai.stockflow.entity.StockBalance;

public record StockBalanceResponse(Long productId, Long warehouseId, long quantity) {

    public static StockBalanceResponse from(StockBalance balance) {
        return new StockBalanceResponse(balance.getProduct().getId(),
                balance.getWarehouse().getId(), balance.getQuantity());
    }
}
