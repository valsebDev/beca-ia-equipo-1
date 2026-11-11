package com.ids.ms_catalog.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestSupport {

	public static final String TEST_SECRET = "test-only-secret-ms-catalog-do-not-use-in-prod-0123456789";
	public static final String ISSUER = "ms-users";
	public static final String AUDIENCE = "mesa-ayuda";
	public static final String ADMIN_ROLE = "ADMIN";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final long ACCESS_TOKEN_SECONDS = 900;

	private SecurityTestSupport() {
	}

	public static RequestPostProcessor admin() {
		return jwtWithRole(ADMIN_ROLE);
	}

	public static RequestPostProcessor jwtWithRole(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	public static String validToken(List<String> roles) {
		return signedToken(TEST_SECRET, ISSUER, AUDIENCE, Instant.now().plusSeconds(ACCESS_TOKEN_SECONDS), roles);
	}

	public static String signedToken(String secret, String issuer, String audience,
			Instant expiresAt, List<String> roles) {
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(
				new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM)));
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject("42")
				.audience(List.of(audience))
				.issuedAt(expiresAt.minusSeconds(ACCESS_TOKEN_SECONDS))
				.expiresAt(expiresAt)
				.claim("roles", roles)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
