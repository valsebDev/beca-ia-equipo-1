package com.ids.ms_users.service;

import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String USER_NOT_FOUND_MESSAGE = "Usuario no encontrado con identificador: ";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales inválidas";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
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
