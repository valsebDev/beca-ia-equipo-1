package com.ids.ms_catalog.exception;

public class NotFoundException extends DomainException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String code, String message) {
		super(code, message);
	}
}
