package com.ids.ms_catalog.exception;

public class ConflictException extends DomainException {

	public ConflictException(String code, String message) {
		super(code, message);
	}
}
