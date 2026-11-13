package com.ids.ms_tickets.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Firma JWT HS256 de prueba con el secreto de application-test.yaml. */
public final class JwtTestTokens {

	public static final String SECRET = "test-only-secret-ms-tickets-0123456789-abcdef";

	private JwtTestTokens() {
	}

	/** Token válido de un SOLICITANTE (sub 10), modificable con el customizer. */
	public static String token(Consumer<JwtClaimsSet.Builder> customizer) {
		return sign(SECRET, customizer);
	}

	public static String sign(String secret, Consumer<JwtClaimsSet.Builder> customizer) {
		Instant now = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("ms-users")
				.audience(List.of("mesa-ayuda"))
				.subject("10")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.claim("roles", List.of("SOLICITANTE"));
		customizer.accept(claims);
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
				new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}
}
