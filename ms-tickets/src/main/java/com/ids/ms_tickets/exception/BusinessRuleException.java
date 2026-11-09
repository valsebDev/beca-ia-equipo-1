package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {

	public BusinessRuleException(String code, String detail) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, code, "Regla de negocio incumplida", detail);
	}
}
