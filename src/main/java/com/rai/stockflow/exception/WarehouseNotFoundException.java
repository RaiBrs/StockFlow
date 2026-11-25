package com.rai.stockflow.exception;

/** Representa a ausência do depósito sem definir uma resposta HTTP no serviço. */
public class WarehouseNotFoundException extends RuntimeException {

    public WarehouseNotFoundException(Long id) {
        super("Depósito não encontrado: " + id);
    }
}
