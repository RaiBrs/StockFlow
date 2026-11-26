package com.rai.stockflow.exception;

public class StockOperationConflictException extends RuntimeException {

    public StockOperationConflictException(String message) {
        super(message);
    }
}
