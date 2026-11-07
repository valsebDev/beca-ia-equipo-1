package com.ids.ms_catalog.exception;

public abstract class DomainException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String code;

	protected DomainException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
