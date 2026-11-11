package com.ids.ms_catalog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Escribe errores de seguridad como application/problem+json con el mismo formato que GlobalExceptionHandler.
 */
public final class ProblemResponseWriter {

	private static final String PROBLEM_BASE_URI = "https://mesa-ayuda.ids/problems/";

	private ProblemResponseWriter() {
	}

	public static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			HttpStatus status, String code, String title, String detail) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", PROBLEM_BASE_URI + code.toLowerCase(Locale.ROOT).replace('_', '-'));
		body.put("title", title);
		body.put("status", status.value());
		body.put("detail", detail);
		body.put("instance", request.getRequestURI());
		body.put("code", code);
		body.put("timestamp", Instant.now().toString());
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
