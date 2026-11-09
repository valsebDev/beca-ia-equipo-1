package com.ids.ms_tickets.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String MALFORMED_DETAIL = "La solicitud no es válida o tiene un formato incorrecto";

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomain(DomainException ex, HttpServletRequest request) {
		return build(ex.getStatus(), ex.getTitle(), ex.getCode(), ex.getMessage(), request, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ProblemFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ProblemFieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", "VALIDATION_ERROR",
				"La solicitud contiene campos inválidos", request, errors);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingRequestHeaderException.class })
	public ProblemDetail handleMalformed(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Solicitud malformada", "MALFORMED_REQUEST", MALFORMED_DETAIL,
				request, null);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleConcurrency(OptimisticLockingFailureException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflicto en la operación", "CONCURRENT_MODIFICATION",
				"El ticket fue modificado por otra operación; vuelva a consultarlo", request, null);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado en {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "INTERNAL_ERROR",
				"Ocurrió un error inesperado", request, null);
	}

	private ProblemDetail build(HttpStatus status, String title, String code, String detail,
			HttpServletRequest request, List<ProblemFieldError> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("https://mesa-ayuda.ids/problems/" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		problem.setProperty("timestamp", Instant.now());
		if (errors != null) {
			problem.setProperty("errors", errors);
		}
		return problem;
	}
}
