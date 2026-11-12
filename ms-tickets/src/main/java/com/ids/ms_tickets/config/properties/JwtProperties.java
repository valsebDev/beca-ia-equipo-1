package com.ids.ms_tickets.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros de validación de los JWT emitidos por ms-users (app.security.jwt.*). */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, String audience) {
}
