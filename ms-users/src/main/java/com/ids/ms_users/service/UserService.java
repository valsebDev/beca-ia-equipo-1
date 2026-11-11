package com.ids.ms_users.service;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.CreateUserRequest;
import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.request.UpdateProfileRequest;
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.dto.response.UserResponse;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.mapper.UserMapper;
import com.ids.ms_users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "Usuario no encontrado con identificador: ";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales inválidas";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final int maxFailedAttempts;
    private final int lockDurationMinutes;

    public UserService(
            UserRepository userRepository,
            TokenService tokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.lock.max-failed-attempts}") int maxFailedAttempts,
            @Value("${app.security.lock.duration-minutes}") int lockDurationMinutes) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        return persistNewUser(request.name(), request.paternalName(), request.maternalName(),
                request.email(), request.password(), Role.SOLICITANTE);
    }

    @Transactional
    public UserResponse createByAdmin(CreateUserRequest request) {
        return persistNewUser(request.name(), request.paternalName(), request.maternalName(),
                request.email(), request.password(), request.role());
    }

    @Transactional
    public UserResponse createAdminIfAbsent(String email, String rawPassword) {
        return persistNewUser("Administrador", "Sistema", null, email, rawPassword, Role.ADMIN);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        validateNotLocked(user);
        validatePassword(user, request.password());
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        String accessToken = tokenService.generateToken(rotated.user());
        return new TokenResponse(accessToken, rotated.refreshToken(), TOKEN_TYPE,
                tokenService.getAccessTtlSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long id) {
        return UserMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = findUser(id);
        user.setName(request.name());
        user.setPaternalName(request.paternalName());
        user.setMaternalName(request.maternalName());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatusByAdmin(Long id, UpdateUserStatusRequest request) {
        User user = findUser(id);
        user.setIsActive(request.isActive());
        if (Boolean.FALSE.equals(request.isActive())) {
            refreshTokenService.revokeAllForUser(id);
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByIdByAdmin(Long id) {
        return UserMapper.toResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsersByAdmin(Role role) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRole(role);
        return users.stream().map(UserMapper::toResponse).toList();
    }

    private UserResponse persistNewUser(String name, String paternalName, String maternalName,
                                        String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User(
                UUID.randomUUID().toString(),
                name,
                paternalName,
                maternalName,
                email,
                passwordEncoder.encode(rawPassword),
                role,
                true,
                LocalDateTime.now()
        );
        return UserMapper.toResponse(userRepository.save(user));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = tokenService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new TokenResponse(accessToken, refreshToken, TOKEN_TYPE, tokenService.getAccessTtlSeconds());
    }

    private void validateNotLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
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
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        userRepository.save(user);
        throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }
}
