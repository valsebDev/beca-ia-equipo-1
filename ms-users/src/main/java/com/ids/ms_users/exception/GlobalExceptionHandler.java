package com.ids.ms_users.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflicto de Datos");
        problem.setType(URI.create("https://ids.com/errors/email-already-exists"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciales Inválidas");
        problem.setType(URI.create("https://ids.com/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, ex.getMessage());
        problem.setTitle("Cuenta Bloqueada Temporalmente");
        problem.setType(URI.create("https://ids.com/errors/account-locked"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleAccountInactive(AccountInactiveException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Cuenta Inactiva");
        problem.setType(URI.create("https://ids.com/errors/account-inactive"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Usuario No Encontrado");
        problem.setType(URI.create("https://ids.com/errors/user-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Uno o más campos tienen errores de validación");
        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://ids.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Inválido",
                (first, second) -> first
            ));

        problem.setProperty("invalidParams", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado en el servidor");
        problem.setTitle("Error Interno del Servidor");
        problem.setType(URI.create("https://ids.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}