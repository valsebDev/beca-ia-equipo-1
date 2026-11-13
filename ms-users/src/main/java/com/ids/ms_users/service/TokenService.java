package com.ids.ms_users.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.ids.ms_users.config.JwtProperties;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public final class TokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MIN_SECRET_BYTES = 32;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;
    private final String audience;

    @Autowired
    public TokenService(JwtProperties properties) {
        this(
                createSigningKey(properties.secret()),
                properties.accessTtlMinutes(),
                properties.issuer(),
                properties.audience());
    }

        public TokenService(
            SecretKey signingKey,
            long expirationMinutes,
            String issuer,
            String audience) {
        if (signingKey == null) {
            throw new IllegalArgumentException("La clave de firma JWT no puede ser nula");
        }
        this.signingKey = signingKey;
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("El usuario debe tener un ID numérico antes de emitir el token");
        }
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + (expirationMinutes * SECONDS_PER_MINUTE * MILLIS_PER_SECOND));
        String fullName = user.getName() + " " + user.getPaternalName();
        if (user.getMaternalName() != null && !user.getMaternalName().isBlank()) {
            fullName += " " + user.getMaternalName();
        }

        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(String.valueOf(user.getId()))
                .claim("userIdentifier", user.getUserIdentifier())
                .claim("email", user.getEmail())
                .claim("name", fullName)
                .claim("roles", List.of(user.getRole().name()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUserIdentifier(String bearerHeader) {
        Claims claims = extractAllClaims(bearerHeader);
        Object identifier = claims.get("userIdentifier");
        if (!(identifier instanceof String userIdentifier) || userIdentifier.isBlank()) {
            throw new InvalidCredentialsException("Token JWT inválido o sin identificador de usuario");
        }
        try {
            Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new InvalidCredentialsException(
                    "Token JWT inválido o sin identificador de usuario", ex);
        }
        return userIdentifier;
    }

    public List<String> extractRoles(String bearerHeader) {
        Claims claims = extractAllClaims(bearerHeader);
        Object rolesClaim = claims.get("roles");
        if (!(rolesClaim instanceof List<?> roleClaims)) {
            return List.of();
        }

        List<String> roles = new ArrayList<>();
        for (Object role : roleClaims) {
            if (!(role instanceof String roleName)) {
                return List.of();
            }
            roles.add(roleName);
        }
        return List.copyOf(roles);
    }

    private Claims extractAllClaims(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidCredentialsException("Cabecera de autorización faltante o con formato inválido");
        }
        String token = bearerHeader.substring(BEARER_PREFIX.length());
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Token JWT inválido o expirado", ex);
        }
    }

    public long getExpirationSeconds() {
        return expirationMinutes * SECONDS_PER_MINUTE;
    }

    private static SecretKey createSigningKey(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres (256 bits)");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
