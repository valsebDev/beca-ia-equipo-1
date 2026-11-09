package com.ids.ms_catalog.exception;

import lombok.Getter;

/**
 * Base de las excepciones de dominio. El código viaja en la propiedad "code" del ProblemDetail.
 */
@Getter
public abstract class DomainException extends RuntimeException {

	private final String code;

	protected DomainException(String code, String message) {
		super(message);
		this.code = code;
	}
}
