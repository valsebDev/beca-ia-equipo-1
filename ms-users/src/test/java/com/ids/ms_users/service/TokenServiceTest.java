package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ids.ms_users.config.properties.JwtProperties;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class TokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-000";
    private static final JwtProperties PROPERTIES = new JwtProperties(
            SECRET, Duration.ofMinutes(15), 7, "ms-users", "mesa-ayuda");

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
    void tokenCarriesConfiguredIssuerAndAudience() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtProperties custom = new JwtProperties(
                SECRET, Duration.ofMinutes(15), 7, "otro-emisor", "otra-audiencia");
        TokenService service = new TokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)), custom);

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).build()
                .decode(service.generateToken(user(42L, Role.ADMIN)));

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("otro-emisor");
        assertThat(jwt.getAudience()).containsExactly("otra-audiencia");
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
        User user = new User("Ana Pérez", "ana@example.com",
                "hash", role, true, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
