package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ids.ms_users.config.JwtProperties;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class TokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-000";
    private static final JwtProperties PROPERTIES = new JwtProperties(
            SECRET, 15, 7, "ms-users", "mesa-ayuda");

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        tokenService = new TokenService(encoder, PROPERTIES);
    }

    @Test
    void generatedTokenIsNotEmpty() {
        assertThat(tokenService.generateToken(user(42L, Role.ADMIN))).isNotBlank();
    }

    @Test
    void accessTtlIsExposedInSeconds() {
        assertThat(tokenService.getAccessTtlSeconds()).isEqualTo(15 * 60L);
    }

    @Test
    void generateRejectsUserWithoutId() {
        User unsaved = user(null, Role.SOLICITANTE);

        assertThatThrownBy(() -> tokenService.generateToken(unsaved))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static User user(Long id, Role role) {
        User user = new User("uid-" + id, "Ana", "Pérez", null, "ana@example.com",
                "hash", role, true, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
