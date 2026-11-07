package com.ids.ms_users.service;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;
    private final String audience;

    public TokenService(
            SecretKey signingKey,
            @Value("${app.security.jwt.access-ttl-minutes:15}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:ms-users}") String issuer,
            @Value("${app.security.jwt.audience:mesa-ayuda}") String audience) {
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
        if (bearerHeader == null || !bearerHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Cabecera de autorización faltante o con formato inválido");
        }
        String token = bearerHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long.parseLong(claims.getSubject());
            String userIdentifier = claims.get("userIdentifier", String.class);
            if (userIdentifier == null || userIdentifier.isBlank()) {
                throw new IllegalArgumentException("El token no contiene un identificador de usuario válido");
            }
            return userIdentifier;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Token JWT inválido o expirado", ex);
        }
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}