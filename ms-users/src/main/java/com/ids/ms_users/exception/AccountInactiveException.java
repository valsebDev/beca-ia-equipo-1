package com.ids.ms_users.exception;

public class AccountInactiveException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AccountInactiveException(String message) {
        super(message);
    }
}