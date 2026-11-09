package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends DomainException {

	public ForbiddenException(String detail) {
		super(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado", detail);
	}
}
