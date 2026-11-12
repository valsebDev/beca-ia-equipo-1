package com.ids.ms_users.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.repository.UserRepository;

@Service
public class AuthService {

    private static final String USER_NOT_FOUND_MESSAGE = "Usuario no encontrado con identificador: ";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales inválidas";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            TokenService tokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(request.email());
        if (found.isEmpty()) {
            // Ejecuta BCrypt igual que con un usuario real para no revelar si el email existe por el tiempo de respuesta.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        User user = found.get();
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
    public void logout(Long userId, String rawRefreshToken) {
        refreshTokenService.revokeOwned(userId, rawRefreshToken);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = tokenService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new TokenResponse(accessToken, TOKEN_TYPE, tokenService.getAccessTtlSeconds(),
                refreshToken, refreshTokenService.getRefreshTtlSeconds());
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }
}
