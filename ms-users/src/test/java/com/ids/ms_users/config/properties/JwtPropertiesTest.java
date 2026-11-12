package com.ids.ms_users.config.properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String VALID_SECRET = "s".repeat(MIN_SECRET_BYTES);

    @Test
    void acceptsSecretOfExactlyMinimumLength() {
        assertThatCode(() -> build(VALID_SECRET)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSecretShorterThanMinimumLength() {
        assertThatThrownBy(() -> build("s".repeat(MIN_SECRET_BYTES - 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> build(null))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtProperties build(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15), 7, "ms-users", "mesa-ayuda");
    }
}
