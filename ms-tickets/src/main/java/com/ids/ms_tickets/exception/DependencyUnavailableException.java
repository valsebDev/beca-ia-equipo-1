package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public class DependencyUnavailableException extends DomainException {

	public DependencyUnavailableException(String detail) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
				"Servicio dependiente no disponible", detail);
	}

	public DependencyUnavailableException(String detail, Throwable cause) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
				"Servicio dependiente no disponible", detail, cause);
	}
}
