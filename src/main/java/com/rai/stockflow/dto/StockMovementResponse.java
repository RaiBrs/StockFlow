package com.rai.stockflow.dto;

import com.rai.stockflow.entity.StockMovement;
import com.rai.stockflow.entity.StockMovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        Long warehouseId,
        Long actorId,
        StockMovementType type,
        long quantity,
        long previousBalance,
        long newBalance,
        String reason,
        Instant occurredAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(movement.getId(),
                movement.getBalance().getProduct().getId(),
                movement.getBalance().getWarehouse().getId(),
                movement.getActor().getId(), movement.getType(), movement.getQuantity(),
                movement.getPreviousBalance(), movement.getNewBalance(),
                movement.getReason(), movement.getOccurredAt());
    }
}
