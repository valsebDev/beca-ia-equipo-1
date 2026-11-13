package com.ids.ms_users.service;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.request.CreateUserRequest;
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.UserResponse;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
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
    private static final String ADMIN_FULL_NAME = "Administrador Sistema";

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }
}

