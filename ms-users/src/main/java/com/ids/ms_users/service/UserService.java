package com.ids.ms_users.service;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.CreateUserRequest;
import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.dto.response.UserResponse;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.mapper.UserMapper;
import com.ids.ms_users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "Usuario no encontrado con identificador: ";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales inválidas";
    private static final String ADMIN_FULL_NAME = "Administrador Sistema";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            TokenService tokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        return persistNewUser(request.fullName(), request.email(), request.password(), Role.SOLICITANTE);
    }

    @Transactional
    public UserResponse createByAdmin(CreateUserRequest request) {
        return persistNewUser(request.fullName(), request.email(), request.password(), request.role());
    }

    @Transactional
    public UserResponse createAdminIfAbsent(String email, String rawPassword) {
        return persistNewUser(ADMIN_FULL_NAME, email, rawPassword, Role.ADMIN);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                || Boolean.FALSE.equals(user.getActive())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = findUser(rotated.userId());
        String accessToken = tokenService.generateToken(user);
        return new TokenResponse(accessToken, TOKEN_TYPE, tokenService.getAccessTtlSeconds(),
                rotated.refreshToken(), refreshTokenService.getRefreshTtlSeconds());
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
    public UserResponse updateStatusByAdmin(Long id, UpdateUserStatusRequest request) {
        User user = findUser(id);
        user.setActive(request.active());
        if (Boolean.FALSE.equals(request.active())) {
            refreshTokenService.revokeAllForUser(user.getId());
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

    private UserResponse persistNewUser(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        User user = new User(
                fullName,
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                role,
                true,
                Instant.now()
        );
        return UserMapper.toResponse(userRepository.save(user));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = tokenService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new TokenResponse(accessToken, TOKEN_TYPE, tokenService.getAccessTtlSeconds(),
                refreshToken, refreshTokenService.getRefreshTtlSeconds());
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }
}

