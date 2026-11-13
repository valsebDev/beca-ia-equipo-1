package com.ids.ms_users.exception;

public class AccountLockedException extends DomainException {
    private static final long serialVersionUID = 1L;

    public AccountLockedException(String message) {
        super(message);
    }
}
