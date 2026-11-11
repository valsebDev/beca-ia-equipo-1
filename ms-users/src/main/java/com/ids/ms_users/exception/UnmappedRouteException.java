package com.ids.ms_users.exception;

import org.springframework.security.core.AuthenticationException;

public class UnmappedRouteException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public UnmappedRouteException() {
        super("Ruta no mapeada y sin autenticación");
    }
}
