package com.ids.ms_users.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;
    private final String audience;

    @Autowired
    public TokenService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-ttl-minutes:15}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:ms-users}") String issuer,
            @Value("${app.security.jwt.audience:mesa-ayuda}") String audience) {
        this(createSigningKey(secret), expirationMinutes, issuer, audience);
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
        Date expiryDate = new Date(nowMillis + (expirationMinutes * 60 * 1000));
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
            throw new InvalidCredentialsException("Token JWT inválido o sin identificador de usuario");
        }
        return userIdentifier;
    }

    public List<String> extractRoles(String bearerHeader) {
        Claims claims = extractAllClaims(bearerHeader);
        Object rolesClaim = claims.get("roles");
        if (!(rolesClaim instanceof List<?> roleClaims)) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        for (Object role : roleClaims) {
            if (!(role instanceof String roleName)) {
                return null;
            }
            roles.add(roleName);
        }
        return List.copyOf(roles);
    }

    private Claims extractAllClaims(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Cabecera de autorización faltante o con formato inválido");
        }
        String token = bearerHeader.substring(7);
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Token JWT inválido o expirado");
        }
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    private static SecretKey createSigningKey(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres (256 bits)");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}