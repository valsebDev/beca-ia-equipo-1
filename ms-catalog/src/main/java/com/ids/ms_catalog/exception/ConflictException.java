package com.ids.ms_catalog.exception;

public class ConflictException extends DomainException {

	private static final long serialVersionUID = 1L;

	public ConflictException(String code, String message) {
		super(code, message);
	}
}
