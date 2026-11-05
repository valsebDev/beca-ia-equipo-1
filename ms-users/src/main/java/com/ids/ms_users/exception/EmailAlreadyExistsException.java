package com.ids.ms_users.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("El correo electrónico ya se encuentra registrado: " + email);
    }
}