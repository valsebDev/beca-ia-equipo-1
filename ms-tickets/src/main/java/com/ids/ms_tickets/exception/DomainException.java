package com.ids.ms_tickets.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String title;

	protected DomainException(HttpStatus status, String code, String title, String detail) {
		super(detail);
		this.status = status;
		this.code = code;
		this.title = title;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getTitle() {
		return title;
	}
}
