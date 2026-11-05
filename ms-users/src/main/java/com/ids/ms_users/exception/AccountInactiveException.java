package com.ids.ms_users.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String message) {
        super(message);
    }
}