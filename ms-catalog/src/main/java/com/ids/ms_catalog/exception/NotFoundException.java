package com.ids.ms_catalog.exception;

public class NotFoundException extends DomainException {

	public NotFoundException(String code, String message) {
		super(code, message);
	}
}
