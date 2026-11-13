package com.ids.ms_catalog.exception;

public class DependencyUnavailableException extends DomainException {

	private static final long serialVersionUID = 1L;
	private static final String CODE = "DEPENDENCY_UNAVAILABLE";

	public DependencyUnavailableException(String message) {
		super(CODE, message);
	}
}
