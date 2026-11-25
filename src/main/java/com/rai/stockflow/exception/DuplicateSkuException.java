package com.rai.stockflow.exception;

/** Indica que o SKU já identifica outro produto. */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("SKU já cadastrado: " + sku);
    }
}
