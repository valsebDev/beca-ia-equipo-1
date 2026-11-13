package com.ids.ms_users.exception;

public class ForbiddenOperationException extends DomainException {
    private static final long serialVersionUID = 1L;

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
