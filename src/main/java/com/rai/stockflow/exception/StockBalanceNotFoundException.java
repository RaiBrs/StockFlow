package com.rai.stockflow.exception;

public class StockBalanceNotFoundException extends RuntimeException {

    public StockBalanceNotFoundException(Long productId, Long warehouseId) {
        super("Saldo não iniciado para produto " + productId + " no depósito " + warehouseId + ".");
    }
}
