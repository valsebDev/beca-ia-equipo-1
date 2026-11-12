package com.ids.ms_catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String PROBLEM_BASE_URI = "https://mesa-ayuda.ids/problems/";
	private static final String NOT_FOUND_TITLE = "Recurso no encontrado";
	private static final Map<Integer, String> FRAMEWORK_TITLES = Map.of(
			404, NOT_FOUND_TITLE,
			405, "Método no permitido",
			415, "Tipo de contenido no soportado");
	private static final Map<Integer, String> FRAMEWORK_DETAILS = Map.of(
			404, NOT_FOUND_TITLE,
			405, "Método HTTP no permitido para este recurso",
			415, "Tipo de contenido no soportado");

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getCode(), NOT_FOUND_TITLE, ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getCode(), "Conflicto con el estado actual", ex.getMessage(), request);
	}

	@ExceptionHandler(DependencyUnavailableException.class)
	ProblemDetail handleDependencyUnavailable(DependencyUnavailableException ex, HttpServletRequest request) {
		return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), "Servicio no disponible", ex.getMessage(), request);
	}

	@ExceptionHandler(DomainValidationException.class)
	ProblemDetail handleDomainValidation(DomainValidationException ex, HttpServletRequest request) {
		ProblemDetail problem = build(
				HttpStatus.BAD_REQUEST, ex.getCode(), "Error de validación", ex.getMessage(), request);
		problem.setProperty("errors", List.of(new FieldViolation(ex.getField(), ex.getMessage())));
		return problem;
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado",
				"No tiene permisos para realizar esta operación", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Error de validación",
				"La solicitud contiene datos no válidos", request);
		problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream().map(this::toViolation).toList());
		return problem;
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	ProblemDetail handleMalformed(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Solicitud mal formada",
				"El cuerpo o los parámetros de la solicitud no son válidos", request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		if (LOG.isErrorEnabled()) {
			LOG.error("Error no controlado en {}", request.getRequestURI(), ex);
		}
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno",
				"Ocurrió un error interno. Intente más tarde", request);
	}

	static ProblemDetail build(
			HttpStatus status, String code, String title, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(PROBLEM_BASE_URI + code.toLowerCase(Locale.ROOT).replace('_', '-')));
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		problem.setProperty("timestamp", Instant.now().toString());
		return problem;
	}

	private FieldViolation toViolation(FieldError error) {
		return new FieldViolation(error.getField(), error.getDefaultMessage());
	}

	public record FieldViolation(String field, String message) {
	}
}
