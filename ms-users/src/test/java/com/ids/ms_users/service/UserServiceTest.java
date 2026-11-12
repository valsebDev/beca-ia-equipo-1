package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.dto.response.UserResponse;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, tokenService, refreshTokenService,
                passwordEncoder);
    }

    @Test
    void loginRejectsUnknownEmailWithSameMessageAsWrongPassword() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithGenericMessage() {
        User user = user(1L, true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
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

        TokenResponse response = userService.login(new LoginRequest(EMAIL, PASSWORD));

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

        assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterUserRequest(FULL_NAME, EMAIL, PASSWORD)))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerStoresEmailInLowercase() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("stored-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new RegisterUserRequest(FULL_NAME, "Ana@Example.com", PASSWORD));

        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    void registerStoresEncodedPasswordAndAssignsSolicitante() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("stored-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(new RegisterUserRequest(FULL_NAME, EMAIL, PASSWORD));

        assertThat(response.fullName()).isEqualTo(FULL_NAME);
        assertThat(response.role()).isEqualTo(Role.SOLICITANTE);
        assertThat(response.active()).isTrue();
    }

    @Test
    void deactivatingUserRevokesAllRefreshTokens() {
        User user = user(9L, true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateStatusByAdmin(9L, new UpdateUserStatusRequest(false));

        assertThat(user.getActive()).isFalse();
        verify(refreshTokenService).revokeAllForUser(eq(9L));
    }

    @Test
    void reactivatingUserDoesNotRevokeTokens() {
        User user = user(9L, false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateStatusByAdmin(9L, new UpdateUserStatusRequest(true));

        assertThat(user.getActive()).isTrue();
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    void updateStatusForUnknownUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStatusByAdmin(99L, new UpdateUserStatusRequest(false)))
                .isInstanceOf(UserNotFoundException.class);
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    private static User user(Long id, boolean active) {
        User user = new User(FULL_NAME, EMAIL, "stored-hash", Role.SOLICITANTE, active, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
