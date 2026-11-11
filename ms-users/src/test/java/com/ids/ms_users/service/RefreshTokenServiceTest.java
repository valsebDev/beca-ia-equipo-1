package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ids.ms_users.config.JwtProperties;
import com.ids.ms_users.domain.RefreshToken;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository,
                new JwtProperties("unused-secret-unused-secret-unused-00", 15, 7, "ms-users", "mesa-ayuda"));
    }

    @Test
    void issueStoresOnlyTheHashOfTheRawToken() {
        User user = user(1L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String raw = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(raw).isNotBlank();
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo(RefreshTokenService.hash(raw))
                .isNotEqualTo(raw);
    }

    @Test
    void rotateRevokesPreviousTokenAndIssuesNewOne() {
        User user = user(1L);
        RefreshToken stored = storedToken(user, "old-token", Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("old-token")))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate("old-token");

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.refreshToken()).isNotEqualTo("old-token");
    }

    @Test
    void rotateWithReusedTokenRevokesAllUserTokens() {
        User user = user(7L);
        RefreshToken stored = storedToken(user, "reused", Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevokedAt(Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("reused")))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(7L), any(Instant.class));
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("missing")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("missing"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rotateRejectsExpiredToken() {
        User user = user(2L);
        RefreshToken stored = storedToken(user, "expired", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("expired")))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRejectsTokenOfInactiveUser() {
        User user = user(3L);
        user.setIsActive(false);
        RefreshToken stored = storedToken(user, "inactive", Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("inactive")))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("inactive"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void revokeIsIdempotentForAlreadyRevokedToken() {
        User user = user(4L);
        RefreshToken stored = storedToken(user, "logout", Instant.now().plus(1, ChronoUnit.DAYS));
        Instant firstRevocation = Instant.now().minusSeconds(30);
        stored.setRevokedAt(firstRevocation);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("logout")))
                .thenReturn(Optional.of(stored));

        refreshTokenService.revoke("logout");

        assertThat(stored.getRevokedAt()).isEqualTo(firstRevocation);
    }

    private static User user(Long id) {
        User user = new User("uid-" + id, "Ana", "Pérez", null, "ana@example.com",
                "hash", Role.SOLICITANTE, true, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static RefreshToken storedToken(User user, String raw, Instant expiresAt) {
        return new RefreshToken(user, RefreshTokenService.hash(raw), expiresAt, Instant.now());
    }
}
