package com.rai.stockflow.exception;

/** Indica que o código já identifica outro depósito. */
public class DuplicateWarehouseCodeException extends RuntimeException {

    public DuplicateWarehouseCodeException(String code) {
        super("Código de depósito já cadastrado: " + code);
    }
}
