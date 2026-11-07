package com.ids.ms_catalog.exception;

/**
 * Regla de negocio que depende de varios campos (por ejemplo, orden de horas del SLA). Responde 400 VALIDATION_ERROR.
 */
public class DomainValidationException extends DomainException {

	private static final long serialVersionUID = 1L;

	private final String field;

	public DomainValidationException(String field, String message) {
		super("VALIDATION_ERROR", message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
