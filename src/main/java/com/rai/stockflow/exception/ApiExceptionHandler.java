package com.rai.stockflow.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte erros conhecidos em respostas HTTP sem expor detalhes do banco. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail invalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado", "Credenciais inválidas.");
    }

    @ExceptionHandler({ProductNotFoundException.class, WarehouseNotFoundException.class,
            UserNotFoundException.class, StockBalanceNotFoundException.class})
    public ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
    }

    @ExceptionHandler({DuplicateSkuException.class, DuplicateWarehouseCodeException.class,
            DuplicateEmailException.class, StockOperationConflictException.class,
            ProductLifecycleConflictException.class, WarehouseLifecycleConflictException.class})
    public ProblemDetail duplicate(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Conflito", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrityConflict() {
        return problem(HttpStatus.CONFLICT, "Conflito",
                "Os dados informados entram em conflito com registros existentes.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidFields(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "A requisição contém campos inválidos.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidBody() {
        return problem(HttpStatus.BAD_REQUEST, "Corpo inválido",
                "O corpo da requisição está ausente ou inválido.");
    }

    @ExceptionHandler(InvalidPageException.class)
    public ProblemDetail invalidPage(InvalidPageException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
