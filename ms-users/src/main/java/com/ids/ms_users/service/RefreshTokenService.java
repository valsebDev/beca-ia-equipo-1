package com.ids.ms_users.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ids.ms_users.config.properties.JwtProperties;
import com.ids.ms_users.domain.RefreshToken;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.repository.RefreshTokenRepository;
import com.ids.ms_users.repository.UserRepository;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final long SECONDS_PER_DAY = 86_400L;
    private static final String INVALID_REFRESH_MESSAGE = "Refresh token inválido o expirado";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository, JwtProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTtlDays = properties.refreshTtlDays();
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlDays * SECONDS_PER_DAY;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        Instant now = Instant.now();
        refreshTokenRepository.save(new RefreshToken(
                user.getId(), hash(rawToken), now.plus(refreshTtlDays, ChronoUnit.DAYS), now));
        return rawToken;
    }

    /**
     * Valida el token, lo revoca y emite uno nuevo. Si el token ya estaba revocado
     * se asume reutilización: se revocan todos los tokens activos del usuario.
     */
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public RotatedToken rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_REFRESH_MESSAGE));
        Instant now = Instant.now();
        Long userId = stored.getUserId();
        if (stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllActiveByUserId(userId, now);
            throw new InvalidCredentialsException(INVALID_REFRESH_MESSAGE);
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            throw new InvalidCredentialsException(INVALID_REFRESH_MESSAGE);
        }
        stored.setRevokedAt(now);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_REFRESH_MESSAGE));
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new InvalidCredentialsException(INVALID_REFRESH_MESSAGE);
        }
        return new RotatedToken(userId, issue(user));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    public record RotatedToken(Long userId, String refreshToken) {
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", ex);
        }
    }
}
