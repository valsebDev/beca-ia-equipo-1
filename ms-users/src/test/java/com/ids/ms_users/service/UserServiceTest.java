package com.ids.ms_users.service;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.AuthResponse;
import com.ids.ms_users.dto.LoginRequest;
import com.ids.ms_users.dto.RegisterUserRequest;
import com.ids.ms_users.dto.UpdateProfileRequest;
import com.ids.ms_users.dto.UpdateUserStatusRequest;
import com.ids.ms_users.dto.UserResponse;
import com.ids.ms_users.exception.AccountInactiveException;
import com.ids.ms_users.exception.AccountLockedException;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    private UserService userService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, tokenService, MAX_FAILED_ATTEMPTS, LOCK_DURATION_MINUTES);
    }

    @Test
    @DisplayName("HU-01: Registro exitoso - guarda usuario con rol SOLICITANTE y estado activo")
    void register_datosValidos_creaUsuarioExitosamente() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "Carlos", "López", "Gómez", "carlos@ids.com", "Password#123"
        );
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        UserResponse response = userService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("carlos@ids.com", response.email());
        assertEquals("Carlos", response.name());
        assertEquals(Role.SOLICITANTE, response.role());
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("HU-01: Registro fallido - lanza EmailAlreadyExistsException si el correo ya existe (409)")
    void register_correoDuplicado_lanzaEmailAlreadyExistsException() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "Carlos", "López", "Gómez", "duplicado@ids.com", "Password#123"
        );
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // When / Then
        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("HU-02: Login exitoso - credenciales válidas devuelven AuthResponse y limpian intentos fallidos")
    void login_credencialesValidas_retornaTokenYReseteaIntentos() {
        // Given
        LoginRequest request = new LoginRequest("usuario@ids.com", "Password#123");
        User user = new User("uuid-123", "Juan", "Pérez", null,
                "usuario@ids.com",
                "$2a$10$w09u7iK3YyYyFh1Yq2mQ7O1Zq8VqX5Yq8VqX5Yq8VqX5Yq8VqX5Yq", // Hash mockeado
                Role.SOLICITANTE, true, LocalDateTime.now());
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Password#123"));
        user.setFailedAttempts(2);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(tokenService.generateToken(any(User.class))).willReturn("jwt-token-valido");
        given(tokenService.getExpirationSeconds()).willReturn(3600L);

        // When
        AuthResponse response = userService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token-valido", response.accessToken());
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("HU-02: Login fallido - contraseña incorrecta incrementa contador y lanza InvalidCredentialsException")
    void login_passwordIncorrecta_incrementaIntentosYLanzaExcepcion() {
        // Given
        LoginRequest request = new LoginRequest("usuario@ids.com", "WrongPassword");
        User user = new User("uuid-123", "Juan", "Pérez", null,
                "usuario@ids.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("CorrectPassword"),
                Role.SOLICITANTE, true, LocalDateTime.now());
        user.setFailedAttempts(1);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        // When / Then
        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        assertEquals(2, user.getFailedAttempts());
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("HU-03: Bloqueo de cuenta - al alcanzar el límite de fallos se bloquea la cuenta (423)")
    void login_quintoIntentoFallido_bloqueaCuentaTemporalmente() {
        // Given
        LoginRequest request = new LoginRequest("usuario@ids.com", "WrongPassword");
        User user = new User("uuid-123", "Juan", "Pérez", null,
                "usuario@ids.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("CorrectPassword"),
                Role.SOLICITANTE, true, LocalDateTime.now());
        user.setFailedAttempts(4); // Al fallar este llegará a 5

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        // When / Then
        assertThrows(AccountLockedException.class, () -> userService.login(request));
        assertEquals(5, user.getFailedAttempts());
        assertNotNull(user.getLockedUntil());
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("HU-04: Perfil - lanzar UserNotFoundException si el identificador no existe (404)")
    void getProfile_usuarioInexistente_lanzaUserNotFoundException() {
        // Given
        String unknownIdentifier = "uuid-no-existe";
        given(userRepository.findByUserIdentifier(unknownIdentifier)).willReturn(Optional.empty());

        // When / Then
        assertThrows(UserNotFoundException.class, () -> userService.getProfile(unknownIdentifier));
    }

    @Test
    @DisplayName("HU-05: Desactivación - actualizar estado a falso borra jwt activo")
    void updateStatus_desactivarUsuario_limpiaSesionActiva() {
        // Given
        String identifier = "uuid-agente";
        User user = new User(identifier, "Agente", "Uno", null, "agente@ids.com", "pass", Role.AGENTE, true, LocalDateTime.now());
        user.setJwtId("active-jwt-id");
        user.setJwtExpiresAt(LocalDateTime.now().plusHours(1));

        given(userRepository.findByUserIdentifier(identifier)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        UserResponse response = userService.updateStatus(identifier, new UpdateUserStatusRequest(false));

        // Then
        assertFalse(response.isActive());
        assertNull(user.getJwtId());
        assertNull(user.getJwtExpiresAt());
        then(userRepository).should().save(user);
    }
}