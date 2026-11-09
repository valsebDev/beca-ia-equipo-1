package com.ids.ms_catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FrameworkExceptionHandler {

	private static final Map<Integer, String> FRAMEWORK_TITLES = Map.of(
			HttpStatus.NOT_FOUND.value(), "Recurso no encontrado",
			HttpStatus.METHOD_NOT_ALLOWED.value(), "Método no permitido",
			HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Tipo de contenido no soportado");
	private static final Map<Integer, String> FRAMEWORK_DETAILS = Map.of(
			HttpStatus.NOT_FOUND.value(), "Recurso no encontrado",
			HttpStatus.METHOD_NOT_ALLOWED.value(), "Método HTTP no permitido para este recurso",
			HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Tipo de contenido no soportado");
	private static final Map<Integer, String> FRAMEWORK_CODES = Map.of(
			HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND",
			HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
			HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "UNSUPPORTED_MEDIA_TYPE");

	@ExceptionHandler(NoResourceFoundException.class)
	ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return frameworkProblem(ex.getStatusCode(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return frameworkProblem(ex.getStatusCode(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		return frameworkProblem(ex.getStatusCode(), request);
	}

	private ProblemDetail frameworkProblem(HttpStatusCode statusCode, HttpServletRequest request) {
		int status = statusCode.value();
		return GlobalExceptionHandler.build(HttpStatus.valueOf(status),
				FRAMEWORK_CODES.getOrDefault(status, "MALFORMED_REQUEST"),
				FRAMEWORK_TITLES.getOrDefault(status, "Solicitud no válida"),
				FRAMEWORK_DETAILS.getOrDefault(status, "La solicitud no es válida"), request);
	}
}
