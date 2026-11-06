package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {

	public NotFoundException(String code, String detail) {
		super(HttpStatus.NOT_FOUND, code, "Recurso no encontrado", detail);
	}
}
