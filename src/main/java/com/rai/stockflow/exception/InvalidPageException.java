package com.rai.stockflow.exception;

public class InvalidPageException extends RuntimeException {

    public InvalidPageException() {
        super("A página deve ser igual ou maior que zero.");
    }
}
