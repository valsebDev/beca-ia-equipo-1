package com.ids.ms_users.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String FIELD_CODE = "code";
    private static final String TIMESTAMP_PROPERTY = "timestamp";

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflicto de Datos");
        problem.setType(URI.create("https://ids.com/errors/email-already-exists"));
        problem.setProperty("code", "EMAIL_ALREADY_EXISTS");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciales Inválidas");
        problem.setType(URI.create("https://ids.com/errors/unauthorized"));
        problem.setProperty("code", "INVALID_CREDENTIALS");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, ex.getMessage());
        problem.setTitle("Cuenta Bloqueada Temporalmente");
        problem.setType(URI.create("https://ids.com/errors/account-locked"));
        problem.setProperty("code", "ACCOUNT_LOCKED");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleAccountInactive(AccountInactiveException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Cuenta Inactiva");
        problem.setType(URI.create("https://ids.com/errors/account-inactive"));
        problem.setProperty("code", "ACCOUNT_INACTIVE");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Usuario No Encontrado");
        problem.setType(URI.create("https://ids.com/errors/user-not-found"));
        problem.setProperty("code", "USER_NOT_FOUND");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperation(ForbiddenOperationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acceso Denegado");
        problem.setType(URI.create("https://ids.com/errors/forbidden"));
        problem.setProperty("code", "FORBIDDEN_OPERATION");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Uno o más campos tienen errores de validación");
        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://ids.com/errors/validation-error"));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldViolation(fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Inválido"))
            .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado en el servidor");
        problem.setTitle("Error Interno del Servidor");
        problem.setType(URI.create("https://ids.com/errors/internal-server-error"));
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}