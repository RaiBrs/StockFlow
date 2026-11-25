package com.rai.stockflow.exception;

/** Representa um conflito de negócio sem definir uma resposta HTTP no serviço. */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
