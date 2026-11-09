package com.ids.ms_users.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmailAlreadyExistsException(String email) {
        super("El correo electrónico ya se encuentra registrado: " + email);
    }
}