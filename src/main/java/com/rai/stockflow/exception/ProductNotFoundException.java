package com.rai.stockflow.exception;

/** Representa a ausência do produto sem definir uma resposta HTTP no serviço. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Produto não encontrado: " + id);
    }
}
