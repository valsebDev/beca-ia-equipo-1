package com.ids.ms_users.service;

import com.ids.ms_users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {

    private final String secret;
    private final long expirationMinutes;
    private final String issuer;

    public TokenService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.security.jwt.issuer}") String issuer) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        // Formato de token autocontenido con claims de negocio requeridos
        String payload = String.format("%s:%s:%s:%s",
        user.getUserIdentifier(), user.getEmail(), user.getRole().name(), issuer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((token + secret).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al calcular hash SHA-256", e);
        }
    }

    public String extractUserIdentifier(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
                return decoded.split(":")[0];
            } catch (Exception e) {
                return token;
            }
        }
        return UUID.randomUUID().toString();
    }
}