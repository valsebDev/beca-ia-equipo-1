package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, tokenService, refreshTokenService,
                passwordEncoder, MAX_FAILED_ATTEMPTS, LOCK_MINUTES);
    }

    @Test
    void loginRejectsUnknownEmailWithSameMessageAsWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void loginWithWrongPasswordIncrementsFailedAttempts() {
        User user = user(1L, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void fifthFailedAttemptLocksAccount() {
        User user = user(1L, true);
        user.setFailedAttempts(MAX_FAILED_ATTEMPTS - 1);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void lockedAccountIsRejectedWithoutCheckingPassword() {
        User user = user(1L, true);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void successfulLoginResetsAttemptsAndIssuesTokens() {
        User user = user(1L, true);
        user.setFailedAttempts(3);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");
        when(tokenService.getAccessTtlSeconds()).thenReturn(900L);

        TokenResponse response = userService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(user.getFailedAttempts()).isZero();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void inactiveUserGetsSameInvalidCredentialsAsWrongPassword() {
        User user = user(1L, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    void deactivatingUserRevokesAllRefreshTokens() {
        User user = user(9L, true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateStatusByAdmin(9L, new UpdateUserStatusRequest(false));

        assertThat(user.getIsActive()).isFalse();
        verify(refreshTokenService).revokeAllForUser(eq(9L));
    }

    private static User user(Long id, boolean active) {
        User user = new User("uid-" + id, "Ana", "Pérez", null, EMAIL,
                "stored-hash", Role.SOLICITANTE, active, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
