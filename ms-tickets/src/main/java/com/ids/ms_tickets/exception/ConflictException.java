package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

	public ConflictException(String code, String detail) {
		super(HttpStatus.CONFLICT, code, "Conflicto en la operación", detail);
	}
}
