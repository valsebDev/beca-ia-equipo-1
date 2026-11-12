package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String FULL_NAME = "Ana Pérez";
    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = "Secreta123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenService, refreshTokenService, passwordEncoder);
    }

    @Test
    void loginRejectsUnknownEmailWithSameMessageAsWrongPassword() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithGenericMessage() {
        User user = user(1L, true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    void successfulLoginIssuesTokensWithExpiry() {
        User user = user(1L, true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");
        when(tokenService.getAccessTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.getRefreshTtlSeconds()).thenReturn(604800L);

        TokenResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.refreshExpiresIn()).isEqualTo(604800L);
    }

    @Test
    void inactiveUserWithCorrectPasswordIsRejectedWithSameMessage() {
        User user = user(1L, false);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(tokenService, never()).generateToken(any(User.class));
    }

    private static User user(Long id, boolean active) {
        User user = new User(FULL_NAME, EMAIL, "stored-hash", Role.SOLICITANTE, active, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
