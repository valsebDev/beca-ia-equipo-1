package com.ids.ms_catalog.config.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

	private static final int MIN_SECRET_BYTES = 32;
	private static final String ISSUER = "ms-users";
	private static final String AUDIENCE = "mesa-ayuda";

	@Test
	void secretoDeMenosDe32BytesFallaAlConstruir() {
		String shortSecret = "x".repeat(MIN_SECRET_BYTES - 1);

		assertThatThrownBy(() -> new JwtProperties(shortSecret, ISSUER, AUDIENCE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("32 bytes");
	}

	@Test
	void secretoDe32BytesEsValido() {
		JwtProperties properties = new JwtProperties("x".repeat(MIN_SECRET_BYTES), ISSUER, AUDIENCE);

		assertThat(properties.secret()).hasSize(MIN_SECRET_BYTES);
	}

	@Test
	void secretoNuloFalla() {
		assertThatThrownBy(() -> new JwtProperties(null, ISSUER, AUDIENCE))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
