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
import com.ids.ms_users.exception.ForbiddenOperationException;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "Usuario no encontrado con identificador: ";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int maxFailedAttempts;
    private final int lockDurationMinutes;

    public UserService(
            UserRepository userRepository,
            TokenService tokenService,
            @Value("${app.security.lock.max-failed-attempts}") int maxFailedAttempts,
            @Value("${app.security.lock.duration-minutes}") int lockDurationMinutes) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                UUID.randomUUID().toString(),
                request.name(),
                request.paternalName(),
                request.maternalName(),
                request.email(),
                encodedPassword,
                Role.SOLICITANTE,
                true,
                LocalDateTime.now()
        );

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales no válidas"));

        validateAccountStatus(user);
        validatePassword(user, request.softPassword());

        user.setFailedAttempts(0);
        user.setLockedUntil(null);

        String token = tokenService.generateToken(user);
        storeTokenMetadata(user);

        return new AuthResponse(
                token,
                "Bearer",
                tokenService.getExpirationSeconds(),
                mapToResponse(user)
        );
    }

    private void validateAccountStatus(User user) {
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("La cuenta está desactivada");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("La cuenta está bloqueada temporalmente por intentos fallidos");
        }
    }

    private void validatePassword(User user, String providedPassword) {
        if (!passwordEncoder.matches(providedPassword, user.getPassword())) {
            handleFailedLoginAttempt(user);
        }
    }

    private void handleFailedLoginAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            userRepository.save(user);
            throw new AccountLockedException("La cuenta se ha bloqueado tras superar el número máximo de intentos");
        }
        userRepository.save(user);
        throw new InvalidCredentialsException("Credenciales no válidas");
    }

    private void storeTokenMetadata(User user) {
        user.setJwtId(UUID.randomUUID().toString());
        user.setJwtExpiresAt(LocalDateTime.now().plusSeconds(tokenService.getExpirationSeconds()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String userIdentifier) {
        User user = userRepository.findByUserIdentifier(userIdentifier)
            .orElseThrow(() -> new UserNotFoundException(
                USER_NOT_FOUND_MESSAGE + userIdentifier));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String userIdentifier, UpdateProfileRequest request) {
        User user = userRepository.findByUserIdentifier(userIdentifier)
            .orElseThrow(() -> new UserNotFoundException(
                USER_NOT_FOUND_MESSAGE + userIdentifier));
        user.setName(request.name());
        user.setPaternalName(request.paternalName());
        user.setMaternalName(request.maternalName());

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Transactional
    public UserResponse updateStatusByAdmin(
            String userIdentifier, UpdateUserStatusRequest request, List<String> requesterRoles) {
        requireAdmin(requesterRoles);
        User user = userRepository.findByUserIdentifier(userIdentifier)
            .orElseThrow(() -> new UserNotFoundException(
                USER_NOT_FOUND_MESSAGE + userIdentifier));
        user.setIsActive(request.isActive());
        if (Boolean.FALSE.equals(request.isActive())) {
            user.setJwtId(null);
            user.setJwtExpiresAt(null);
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    private void requireAdmin(List<String> roles) {
        if (roles == null || !roles.contains(Role.ADMIN.name())) {
            throw new ForbiddenOperationException("Solo un usuario con rol ADMIN puede cambiar estados");
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getUserIdentifier(),
                user.getName(),
                user.getPaternalName(),
                user.getMaternalName(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}